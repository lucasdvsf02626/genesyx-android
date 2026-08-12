package com.genesyx.app.notifications

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

/** The next-occurrence maths for a customer-set supplement reminder: today if the time is still
 *  ahead, otherwise tomorrow — the same shape the daily reminders use. */
class SupplementReminderSchedulerTest {

    private val zone = ZoneId.of("Europe/London")
    private fun at(y: Int, mo: Int, d: Int, h: Int, mi: Int) = ZonedDateTime.of(y, mo, d, h, mi, 0, 0, zone)

    @Test
    fun `a time later today fires today`() {
        // Now 08:00, reminder 09:00 → same day.
        val next = SupplementReminderScheduler.nextOccurrence(9 * 60, at(2026, 8, 12, 8, 0))
        assertEquals(at(2026, 8, 12, 9, 0), next)
    }

    @Test
    fun `a time already past today rolls to tomorrow`() {
        val next = SupplementReminderScheduler.nextOccurrence(9 * 60, at(2026, 8, 12, 9, 30))
        assertEquals(at(2026, 8, 13, 9, 0), next)
    }

    @Test
    fun `the exact minute now still rolls forward, never a zero delay`() {
        val next = SupplementReminderScheduler.nextOccurrence(9 * 60, at(2026, 8, 12, 9, 0))
        assertEquals(at(2026, 8, 13, 9, 0), next)
    }

    @Test
    fun `minutes-of-day maps to the right hour and minute`() {
        val next = SupplementReminderScheduler.nextOccurrence(21 * 60 + 45, at(2026, 8, 12, 8, 0))
        assertEquals(at(2026, 8, 12, 21, 45), next)
    }

    @Test
    fun `the work name is namespaced per supplement id`() {
        assertEquals("supp-reminder-abc", SupplementReminderScheduler.workName("abc"))
    }
}
