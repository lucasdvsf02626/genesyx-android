package com.genesyx.app.ui.insights

import com.genesyx.app.domain.model.DailyLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Validates the real hydration card: weekly bars, the rolling week-over-week delta, and the copy. */
class HydrationInsightLogicTest {

    // Sunday — the current Mon–Sun week is 2026-06-15..21 and the rolling window is the same seven.
    private val today = LocalDate.of(2026, 6, 21)
    private val monday = LocalDate.of(2026, 6, 15)

    /** [ml] of water on each of the seven days ending [endingDaysAgo] days before today. */
    private fun window(ml: Int, endingDaysAgo: Long): Map<LocalDate, DailyLog> =
        (0L until 7L).associate { today.minusDays(endingDaysAgo + it) to DailyLog(waterMl = ml) }

    @Test
    fun `no water in the last seven days is the empty state`() {
        val r = HydrationInsightLogic.compute(emptyMap(), today)
        assertFalse(r.hasData)
        assertNull(r.avgMlPerDay)
        assertNull(r.deltaMlPerDay)
        assertEquals(HydrationInsights().insight, r.insight)
    }

    @Test
    fun `water older than the window does not count as data`() {
        val old = mapOf(today.minusDays(9) to DailyLog(waterMl = 2000))
        assertFalse(HydrationInsightLogic.compute(old, today).hasData)
    }

    @Test
    fun `the average is millilitres per day across all seven days, logged or not`() {
        // Two days of 2100ml, five days of nothing: 4200 / 7 = 600, not 2100.
        val sparse = mapOf(
            today to DailyLog(waterMl = 2100),
            today.minusDays(1) to DailyLog(waterMl = 2100),
        )
        val r = HydrationInsightLogic.compute(sparse, today)
        assertTrue(r.hasData)
        assertEquals(600, r.avgMlPerDay)
    }

    @Test
    fun `with only one week of data there is no delta`() {
        val r = HydrationInsightLogic.compute(window(2100, 0), today)
        assertEquals(2100, r.avgMlPerDay)
        assertNull("nothing to compare against yet", r.deltaMlPerDay)
        assertTrue(r.insight.contains("another week"))
    }

    @Test
    fun `a rise is reported against the previous seven days`() {
        val r = HydrationInsightLogic.compute(window(2100, 0) + window(1400, 7), today)
        assertEquals(700, r.deltaMlPerDay)
        assertEquals("You're averaging 2100ml a day, 700ml more than the previous seven days.", r.insight)
    }

    @Test
    fun `a drop is reported plainly, without scolding`() {
        val r = HydrationInsightLogic.compute(window(1400, 0) + window(2100, 7), today)
        assertEquals(-700, r.deltaMlPerDay)
        assertEquals("You're averaging 1400ml a day, 700ml less than the previous seven days.", r.insight)
    }

    @Test
    fun `a small change reads as holding steady`() {
        // 7 x 2100 = 14700 vs 7 x 2100 + 140 -> a 20ml/day difference, inside the 50ml band.
        val nudged = window(2100, 0) + mapOf(today to DailyLog(waterMl = 2240))
        val r = HydrationInsightLogic.compute(nudged + window(2100, 7), today)
        assertEquals(20, r.deltaMlPerDay)
        assertTrue(r.insight.contains("holding steady"))
    }

    @Test
    fun `bars are the current monday-to-sunday week as a percentage of the goal`() {
        val week = mapOf(
            monday to DailyLog(waterMl = 1200), // half the 2400ml goal
            monday.plusDays(6) to DailyLog(waterMl = 4800), // over goal — clamped
        )
        val r = HydrationInsightLogic.compute(week, today)
        assertEquals(listOf(50, 0, 0, 0, 0, 0, 100), r.bars)
    }
}
