package com.genesyx.app.notifications

import com.genesyx.app.notifications.model.NotificationSettings
import com.genesyx.app.notifications.model.ReminderKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class ReminderPolicyTest {

    private val zone = ZoneId.of("Europe/London")
    private fun at(y: Int, mo: Int, d: Int, h: Int, mi: Int) =
        ZonedDateTime.of(y, mo, d, h, mi, 0, 0, zone)

    private val settings = NotificationSettings()

    // A context that passes every universal gate, so per-kind rules are what a test isolates.
    private fun ctx(
        signedIn: Boolean = true,
        enabled: Boolean = true,
        foreground: Boolean = false,
        loggedToday: Boolean = false,
        loggedYesterday: Boolean = false,
        activeUser: Boolean = true,
        logs7: Int = 5,
        cycle: Boolean = true,
        awayDays: Long? = 0,
    ) = ReminderPolicy.PostContext(
        signedIn = signedIn,
        notificationsEnabledAtOs = enabled,
        appInForeground = foreground,
        loggedToday = loggedToday,
        loggedYesterday = loggedYesterday,
        activeUser = activeUser,
        logsInLast7Days = logs7,
        hasCycleSettings = cycle,
        daysSinceLastOpen = awayDays,
    )

    // ── nextOccurrence ────────────────────────────────────────────────────────

    @Test
    fun `daily reminder later today fires today`() {
        // Now 20:00, reminder 21:00 -> same day.
        val next = ReminderPolicy.nextOccurrence(ReminderKind.DAILY_LOG, settings, at(2026, 6, 15, 20, 0))
        assertEquals(at(2026, 6, 15, 21, 0), next)
    }

    @Test
    fun `daily reminder already past today rolls to tomorrow`() {
        val next = ReminderPolicy.nextOccurrence(ReminderKind.DAILY_LOG, settings, at(2026, 6, 15, 21, 30))
        assertEquals(at(2026, 6, 16, 21, 0), next)
    }

    @Test
    fun `daily reminder skips unselected days`() {
        // Only Mondays selected. Now is Monday 22:00, so next is the following Monday.
        val mondayOnly = settings.copy(dailyLogDays = setOf(DayOfWeek.MONDAY))
        val nowMon = at(2026, 6, 15, 22, 0) // 2026-06-15 is a Monday
        val next = ReminderPolicy.nextOccurrence(ReminderKind.DAILY_LOG, mondayOnly, nowMon)
        assertEquals(DayOfWeek.MONDAY, next.dayOfWeek)
        assertEquals(at(2026, 6, 22, 21, 0), next)
    }

    @Test
    fun `weekly insights lands on its configured day`() {
        val next = ReminderPolicy.nextOccurrence(ReminderKind.WEEKLY_INSIGHTS, settings, at(2026, 6, 15, 12, 0))
        assertEquals(DayOfWeek.SUNDAY, next.dayOfWeek)
        assertEquals(at(2026, 6, 21, 10, 0), next) // next Sunday 10:00
    }

    // ── quiet hours (overnight wrap) ──────────────────────────────────────────

    @Test
    fun `quiet hours wrap across midnight`() {
        // Default 22:00 -> 07:00.
        assertTrue(ReminderPolicy.inQuietHours(LocalTime.of(23, 0), settings))
        assertTrue(ReminderPolicy.inQuietHours(LocalTime.of(6, 0), settings))
        assertFalse(ReminderPolicy.inQuietHours(LocalTime.of(7, 0), settings)) // end is exclusive
        assertFalse(ReminderPolicy.inQuietHours(LocalTime.of(12, 0), settings))
        assertTrue(ReminderPolicy.inQuietHours(LocalTime.of(22, 0), settings)) // start inclusive
    }

    @Test
    fun `quiet hours disabled never suppresses`() {
        val off = settings.copy(quietHoursEnabled = false)
        assertFalse(ReminderPolicy.inQuietHours(LocalTime.of(23, 0), off))
    }

    // ── shouldPost universal gates ────────────────────────────────────────────

    @Test
    fun `signed out suppresses everything`() {
        assertFalse(ReminderPolicy.shouldPost(ReminderKind.DAILY_LOG, at(2026, 6, 15, 21, 0), settings, ctx(signedIn = false)))
    }

    @Test
    fun `foreground suppresses`() {
        assertFalse(ReminderPolicy.shouldPost(ReminderKind.DAILY_LOG, at(2026, 6, 15, 21, 0), settings, ctx(foreground = true)))
    }

    @Test
    fun `os-disabled suppresses`() {
        assertFalse(ReminderPolicy.shouldPost(ReminderKind.DAILY_LOG, at(2026, 6, 15, 21, 0), settings, ctx(enabled = false)))
    }

    @Test
    fun `quiet hours suppress a daily reminder`() {
        // 23:00 is inside default quiet hours.
        assertFalse(ReminderPolicy.shouldPost(ReminderKind.DAILY_LOG, at(2026, 6, 15, 23, 0), settings, ctx()))
    }

    @Test
    fun `daily reminder posts at its time when nothing suppresses it`() {
        assertTrue(ReminderPolicy.shouldPost(ReminderKind.DAILY_LOG, at(2026, 6, 15, 21, 0), settings, ctx()))
    }

    // ── the highest-value rule: already logged today ──────────────────────────

    @Test
    fun `daily reminder is suppressed once she has logged today`() {
        assertFalse(ReminderPolicy.shouldPost(ReminderKind.DAILY_LOG, at(2026, 6, 15, 21, 0), settings, ctx(loggedToday = true)))
    }

    // ── per-kind gates ────────────────────────────────────────────────────────

    @Test
    fun `missed log needs an active user who did not log yesterday`() {
        val now = at(2026, 6, 15, 9, 0)
        assertTrue(ReminderPolicy.shouldPost(ReminderKind.MISSED_LOG, now, settings, ctx()))
        assertFalse("logged yesterday -> nothing open", ReminderPolicy.shouldPost(ReminderKind.MISSED_LOG, now, settings, ctx(loggedYesterday = true)))
        assertFalse("lapsed user -> not a slip", ReminderPolicy.shouldPost(ReminderKind.MISSED_LOG, now, settings, ctx(activeUser = false)))
    }

    @Test
    fun `missed log holds off within three days of the last one`() {
        val now = at(2026, 6, 15, 9, 0)
        val recent = settings.copy(lastMissedLogEpochDay = now.toLocalDate().minusDays(1).toEpochDay())
        assertFalse(ReminderPolicy.shouldPost(ReminderKind.MISSED_LOG, now, recent, ctx()))
    }

    @Test
    fun `weekly insights needs at least two logs in the week`() {
        val sunday = at(2026, 6, 21, 10, 0)
        assertTrue(ReminderPolicy.shouldPost(ReminderKind.WEEKLY_INSIGHTS, sunday, settings, ctx(logs7 = 2)))
        assertFalse(ReminderPolicy.shouldPost(ReminderKind.WEEKLY_INSIGHTS, sunday, settings, ctx(logs7 = 1)))
    }

    @Test
    fun `weekly insights only fires on its day`() {
        val saturday = at(2026, 6, 20, 10, 0)
        assertFalse(ReminderPolicy.shouldPost(ReminderKind.WEEKLY_INSIGHTS, saturday, settings, ctx(logs7 = 5)))
    }

    @Test
    fun `re-engagement respects away-window, interval and hard stop`() {
        val now = at(2026, 6, 15, 18, 0)
        assertTrue(ReminderPolicy.shouldPost(ReminderKind.REENGAGEMENT, now, settings, ctx(awayDays = 3)))
        assertFalse("too soon", ReminderPolicy.shouldPost(ReminderKind.REENGAGEMENT, now, settings, ctx(awayDays = 2)))
        assertFalse("gone for good", ReminderPolicy.shouldPost(ReminderKind.REENGAGEMENT, now, settings, ctx(awayDays = 31)))
        val ignored = settings.copy(consecutiveIgnoredReengagements = 3)
        assertFalse("hard stop after 3 ignored", ReminderPolicy.shouldPost(ReminderKind.REENGAGEMENT, now, ignored, ctx(awayDays = 5)))
        val recent = settings.copy(lastReengagementEpochDay = now.toLocalDate().minusDays(2).toEpochDay())
        assertFalse("within 7 days of the last", ReminderPolicy.shouldPost(ReminderKind.REENGAGEMENT, now, recent, ctx(awayDays = 5)))
    }

    // ── daily cap ─────────────────────────────────────────────────────────────

    @Test
    fun `daily cap suppresses non-reengagement once reached`() {
        val now = at(2026, 6, 15, 21, 0)
        val capped = settings.copy(
            notificationsPostedToday = NotificationSettings.DAILY_CAP,
            postedCounterEpochDay = now.toLocalDate().toEpochDay(),
        )
        assertFalse(ReminderPolicy.shouldPost(ReminderKind.DAILY_LOG, now, capped, ctx()))
        // Re-engagement is exempt from the cap.
        assertTrue(ReminderPolicy.shouldPost(ReminderKind.REENGAGEMENT, at(2026, 6, 15, 18, 0), capped, ctx(awayDays = 4)))
    }

    @Test
    fun `a cap counter from yesterday does not block today`() {
        val now = at(2026, 6, 15, 21, 0)
        val stale = settings.copy(
            notificationsPostedToday = 9,
            postedCounterEpochDay = now.toLocalDate().minusDays(1).toEpochDay(),
        )
        assertTrue(ReminderPolicy.shouldPost(ReminderKind.DAILY_LOG, now, stale, ctx()))
    }
}
