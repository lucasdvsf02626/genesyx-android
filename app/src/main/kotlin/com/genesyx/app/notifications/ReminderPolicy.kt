package com.genesyx.app.notifications

import com.genesyx.app.notifications.model.NotificationSettings
import com.genesyx.app.notifications.model.ReminderKind
import java.time.LocalTime
import java.time.ZonedDateTime

/**
 * The pure brain of the reminder engine: given the settings, the clock, and a snapshot of runtime
 * state, it decides *when* a kind next runs ([nextOccurrence]) and *whether* it may post right now
 * ([shouldPost]). No Android, no IO — every rule here is a plain unit test, because these are the
 * rules that, wrong, either spam a user or silently never fire.
 */
object ReminderPolicy {

    /** Fixed clock positions for kinds without their own time picker in v1.1. */
    val MISSED_LOG_TIME: LocalTime = LocalTime.of(9, 0)
    val REENGAGEMENT_CHECK_TIME: LocalTime = LocalTime.of(18, 0)

    private const val REENGAGEMENT_MIN_AWAY_DAYS = 3L
    private const val REENGAGEMENT_INTERVAL_DAYS = 7L
    private const val REENGAGEMENT_MAX_AWAY_DAYS = 30L
    private const val REENGAGEMENT_HARD_STOP = 3
    private const val MISSED_LOG_MIN_GAP_DAYS = 3L

    /** A snapshot of the runtime facts [shouldPost] needs but cannot compute itself. */
    data class PostContext(
        val signedIn: Boolean,
        val notificationsEnabledAtOs: Boolean,
        val appInForeground: Boolean,
        val loggedToday: Boolean,
        val loggedYesterday: Boolean,
        /** Logged ≥3 of the last 14 days — an active user who slipped, not a lapsed one. */
        val activeUser: Boolean,
        val logsInLast7Days: Int,
        val hasCycleSettings: Boolean,
        val daysSinceLastOpen: Long?,
    )

    /**
     * The next moment [kind] should run, strictly after [now]. The worker enqueues a fresh one-time
     * request for this instant every time it runs — a self-rescheduling chain — so there is no
     * dependence on `PeriodicWorkRequest`'s fuzzy delivery window.
     */
    fun nextOccurrence(
        kind: ReminderKind,
        settings: NotificationSettings,
        now: ZonedDateTime,
    ): ZonedDateTime = when (kind) {
        ReminderKind.DAILY_LOG -> nextAt(now, settings.dailyLogTime, settings.dailyLogDays)
        ReminderKind.WEEKLY_INSIGHTS -> nextAt(now, settings.weeklyInsightsTime, setOf(settings.weeklyInsightsDay))
        ReminderKind.HYDRATION -> nextAt(now, settings.hydrationTime, ALL_DAYS)
        ReminderKind.MISSED_LOG -> nextAt(now, MISSED_LOG_TIME, ALL_DAYS)
        ReminderKind.REENGAGEMENT -> nextAt(now, REENGAGEMENT_CHECK_TIME, ALL_DAYS)
        // Phase-transition scheduling is reserved for a later version; never schedule it here.
        ReminderKind.NUTRITION -> nextAt(now, settings.hydrationTime, ALL_DAYS)
    }

    /**
     * Whether [kind] may post at [now]. Rules are evaluated in order; the first that matches
     * suppresses. The ordering is deliberate — cheap universal gates (signed out, disabled,
     * foreground, quiet hours) before the per-kind ones.
     */
    fun shouldPost(
        kind: ReminderKind,
        now: ZonedDateTime,
        settings: NotificationSettings,
        ctx: PostContext,
    ): Boolean {
        if (!ctx.signedIn) return false
        if (!ctx.notificationsEnabledAtOs) return false
        if (!settings.isEnabled(kind)) return false
        if (ctx.appInForeground) return false
        if (inQuietHours(now.toLocalTime(), settings)) return false

        val today = now.toLocalDate().toEpochDay()
        if (!inScheduledWindow(kind, now, settings)) return false
        if (!kindGate(kind, today, ctx, settings)) return false

        // Global daily cap — re-engagement is exempt because rule 8 already keeps it rare and alone.
        if (kind != ReminderKind.REENGAGEMENT && postedToday(settings, today) >= NotificationSettings.DAILY_CAP) {
            return false
        }
        return true
    }

    /** True when [t] falls inside the quiet window, handling the overnight wrap (22:00 → 07:00). */
    fun inQuietHours(t: LocalTime, settings: NotificationSettings): Boolean {
        if (!settings.quietHoursEnabled) return false
        val start = settings.quietHoursStart
        val end = settings.quietHoursEnd
        return if (start <= end) {
            t >= start && t < end
        } else {
            // Wraps midnight: quiet if after start OR before end.
            t >= start || t < end
        }
    }

    /** The count of notifications posted *today* — zero if the stored counter is for an older day. */
    fun postedToday(settings: NotificationSettings, todayEpochDay: Long): Int =
        if (settings.postedCounterEpochDay == todayEpochDay) settings.notificationsPostedToday else 0

    /** Day-of-week / weekly-day scheduling window for time-locked kinds. */
    private fun inScheduledWindow(kind: ReminderKind, now: ZonedDateTime, settings: NotificationSettings): Boolean =
        when (kind) {
            ReminderKind.DAILY_LOG -> now.dayOfWeek in settings.dailyLogDays
            ReminderKind.WEEKLY_INSIGHTS -> now.dayOfWeek == settings.weeklyInsightsDay
            else -> true
        }

    private fun kindGate(
        kind: ReminderKind,
        today: Long,
        ctx: PostContext,
        settings: NotificationSettings,
    ): Boolean = when (kind) {
        ReminderKind.DAILY_LOG -> !ctx.loggedToday

        ReminderKind.MISSED_LOG -> when {
            ctx.loggedToday || ctx.loggedYesterday -> false // nothing left open
            !ctx.activeUser -> false // a lapsed user, not a slip
            settings.lastMissedLogEpochDay?.let { today - it < MISSED_LOG_MIN_GAP_DAYS } == true -> false
            else -> true
        }

        ReminderKind.WEEKLY_INSIGHTS -> ctx.logsInLast7Days >= 2

        ReminderKind.NUTRITION -> ctx.hasCycleSettings

        ReminderKind.REENGAGEMENT -> {
            val away = ctx.daysSinceLastOpen
            when {
                away == null || away < REENGAGEMENT_MIN_AWAY_DAYS -> false
                away > REENGAGEMENT_MAX_AWAY_DAYS -> false // she's gone; cancel, don't ping
                settings.consecutiveIgnoredReengagements >= REENGAGEMENT_HARD_STOP -> false
                settings.lastReengagementEpochDay?.let { today - it < REENGAGEMENT_INTERVAL_DAYS } == true -> false
                else -> true
            }
        }

        ReminderKind.HYDRATION -> true
    }

    private fun nextAt(now: ZonedDateTime, time: LocalTime, allowedDays: Set<java.time.DayOfWeek>): ZonedDateTime {
        var candidate = now.toLocalDate().atTime(time).atZone(now.zone)
        if (!candidate.isAfter(now)) candidate = candidate.plusDays(1)
        while (candidate.dayOfWeek !in allowedDays) candidate = candidate.plusDays(1)
        return candidate
    }

    private val ALL_DAYS: Set<java.time.DayOfWeek> = java.time.DayOfWeek.entries.toSet()
}
