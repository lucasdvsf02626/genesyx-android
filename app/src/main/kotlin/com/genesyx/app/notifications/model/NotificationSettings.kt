package com.genesyx.app.notifications.model

import java.time.DayOfWeek
import java.time.LocalTime

/**
 * Everything the reminder engine needs to decide *whether* and *when* to post, in one immutable
 * value. The settings screen writes it; the scheduler and the suppression policy read it. Kept pure
 * (only `java.time`) so both of those stay JVM-testable.
 *
 * [masterEnabled] mirrors the existing `push_enabled` DataStore flag — the single kill switch behind
 * the per-kind toggles. The trailing counters are engine bookkeeping (daily cap, re-engagement
 * pacing, grace window), not user-facing preferences.
 */
data class NotificationSettings(
    val masterEnabled: Boolean = true,
    val enabledKinds: Set<ReminderKind> = DEFAULT_ENABLED,

    val dailyLogTime: LocalTime = LocalTime.of(21, 0),
    val dailyLogDays: Set<DayOfWeek> = DayOfWeek.entries.toSet(),

    val weeklyInsightsTime: LocalTime = LocalTime.of(10, 0),
    val weeklyInsightsDay: DayOfWeek = DayOfWeek.SUNDAY,

    val hydrationTime: LocalTime = LocalTime.of(11, 0),

    val quietHoursEnabled: Boolean = true,
    val quietHoursStart: LocalTime = LocalTime.of(22, 0),
    val quietHoursEnd: LocalTime = LocalTime.of(7, 0),

    /** Set when the permission sheet is first accepted — disambiguates "never asked" from "denied". */
    val lastPromptedAt: Long? = null,
    /** Epoch-millis of the first permission grant — powers the 24h post-grant quiet window. */
    val firstGrantedAt: Long? = null,

    // ── Engine counters (DataStore-persisted) ─────────────────────────────────
    val notificationsPostedToday: Int = 0,
    /** The epoch-day [notificationsPostedToday] is counting for; a new day resets the count. */
    val postedCounterEpochDay: Long = 0,
    val lastReengagementEpochDay: Long? = null,
    val consecutiveIgnoredReengagements: Int = 0,
    val lastOpenedEpochDay: Long? = null,
    val lastMissedLogEpochDay: Long? = null,
) {
    fun isEnabled(kind: ReminderKind): Boolean = masterEnabled && kind in enabledKinds

    companion object {
        /** Reminders on by default the first time she grants permission. Wellness nudges stay opt-in. */
        val DEFAULT_ENABLED: Set<ReminderKind> = setOf(
            ReminderKind.DAILY_LOG,
            ReminderKind.MISSED_LOG,
            ReminderKind.WEEKLY_INSIGHTS,
            ReminderKind.REENGAGEMENT,
        )

        /** The global daily ceiling, across all channels ([ReminderKind.REENGAGEMENT] excepted). */
        const val DAILY_CAP = 2
    }
}
