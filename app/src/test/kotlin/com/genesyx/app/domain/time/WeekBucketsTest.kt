package com.genesyx.app.domain.time

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/** The edges the two old copies of this logic were each quietly getting right on their own. */
class WeekBucketsTest {

    @Test
    fun `a monday is its own week start`() {
        val monday = LocalDate.of(2026, 6, 15)
        assertEquals(monday, WeekBuckets.weekStart(monday))
    }

    @Test
    fun `sunday belongs to the week that began the monday before, not the one starting tomorrow`() {
        // The whole reason weeks are Monday-start: Sunday is the END of her week, not the start.
        assertEquals(LocalDate.of(2026, 6, 15), WeekBuckets.weekStart(LocalDate.of(2026, 6, 21)))
    }

    @Test
    fun `a week can straddle the new year`() {
        // Thu 2026-12-31 sits in the week beginning Mon 2026-12-28, which runs into January.
        assertEquals(LocalDate.of(2026, 12, 28), WeekBuckets.weekStart(LocalDate.of(2026, 12, 31)))
        assertEquals(LocalDate.of(2026, 12, 28), WeekBuckets.weekStart(LocalDate.of(2027, 1, 3)))
        assertEquals(LocalDate.of(2027, 1, 3), WeekBuckets.weekDays(LocalDate.of(2026, 12, 31)).last())
    }

    @Test
    fun `a week can straddle a leap day`() {
        // 2028 is a leap year; Mon 2028-02-28 -> the week contains the 29th.
        val week = WeekBuckets.weekDays(LocalDate.of(2028, 2, 28))
        assertEquals(LocalDate.of(2028, 2, 28), week.first())
        assertEquals(LocalDate.of(2028, 2, 29), week[1])
        assertEquals(LocalDate.of(2028, 3, 5), week.last())
    }

    @Test
    fun `week days are seven, monday first, sunday last`() {
        val week = WeekBuckets.weekDays(LocalDate.of(2026, 6, 18))
        assertEquals(7, week.size)
        assertEquals(LocalDate.of(2026, 6, 15), week.first())
        assertEquals(LocalDate.of(2026, 6, 21), week.last())
        assertEquals(week, week.sorted())
    }
}
