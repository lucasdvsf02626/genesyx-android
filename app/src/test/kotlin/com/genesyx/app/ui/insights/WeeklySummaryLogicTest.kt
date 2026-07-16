package com.genesyx.app.ui.insights

import com.genesyx.app.domain.model.DailyLog
import com.genesyx.app.domain.model.EnergyLevel
import com.genesyx.app.domain.model.Mood
import com.genesyx.app.domain.model.Supplement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Validates the weekly summary: logged-day counts against last week, and the deltas that appear only
 * when both weeks hold the data to compare.
 */
class WeeklySummaryLogicTest {

    // Sunday — this week is 2026-06-15..21, last week is 2026-06-08..14.
    private val today = LocalDate.of(2026, 6, 21)
    private val thisMon = LocalDate.of(2026, 6, 15)
    private val lastMon = LocalDate.of(2026, 6, 8)

    @Test
    fun `no logs this week is the empty state`() {
        val r = WeeklySummaryLogic.compute(emptyMap(), today)
        assertFalse(r.hasData)
        assertEquals(0, r.daysLogged)
        assertEquals(WeeklySummaryInsights().insight, r.insight)
    }

    @Test
    fun `days logged count meaningful days in each week`() {
        val logs = mapOf(
            thisMon to DailyLog(waterMl = 500),
            thisMon.plusDays(1) to DailyLog(mood = Mood.GOOD),
            thisMon.plusDays(2) to DailyLog(), // empty — not meaningful, not counted
            lastMon to DailyLog(sleepMinutes = 480),
        )
        val r = WeeklySummaryLogic.compute(logs, today)
        assertTrue(r.hasData)
        assertEquals(2, r.daysLogged)
        assertEquals(1, r.prevDaysLogged)
    }

    @Test
    fun `hydration delta is null unless both weeks logged water`() {
        val onlyThisWeek = mapOf(thisMon to DailyLog(waterMl = 2100))
        assertNull(WeeklySummaryLogic.compute(onlyThisWeek, today).hydrationDeltaMlPerDay)

        // This week averages 2100/7 = 300; last week 700/7 = 100. Delta = +200.
        val bothWeeks = mapOf(
            thisMon to DailyLog(waterMl = 2100),
            lastMon to DailyLog(waterMl = 700),
        )
        assertEquals(200, WeeklySummaryLogic.compute(bothWeeks, today).hydrationDeltaMlPerDay)
    }

    @Test
    fun `sleep delta averages over logged nights and needs both weeks`() {
        val onlyThisWeek = mapOf(thisMon to DailyLog(sleepMinutes = 480))
        assertNull(WeeklySummaryLogic.compute(onlyThisWeek, today).sleepDeltaMinutes)

        // This week one night of 8h (480); last week one night of 7h (420). Delta = +60.
        val bothWeeks = mapOf(
            thisMon to DailyLog(sleepMinutes = 480),
            lastMon to DailyLog(sleepMinutes = 420),
        )
        assertEquals(60, WeeklySummaryLogic.compute(bothWeeks, today).sleepDeltaMinutes)
    }

    @Test
    fun `supplement days delta counts days with any plan supplement`() {
        val folate = setOf(Supplement.FOLATE.wireName)
        val logs = mapOf(
            thisMon to DailyLog(supplements = folate),
            thisMon.plusDays(1) to DailyLog(supplements = folate),
            lastMon to DailyLog(supplements = folate),
        )
        // Two days this week, one last week -> +1.
        assertEquals(1, WeeklySummaryLogic.compute(logs, today).supplementDaysDelta)
    }

    @Test
    fun `mood and energy tally into a plain line`() {
        val logs = mapOf(
            thisMon to DailyLog(mood = Mood.GOOD, energy = EnergyLevel.HIGH),
            thisMon.plusDays(1) to DailyLog(mood = Mood.GOOD, energy = EnergyLevel.NORMAL),
        )
        val r = WeeklySummaryLogic.compute(logs, today)
        assertEquals("Mostly good, energy often high", r.moodEnergyLine)
    }

    @Test
    fun `the mood-energy line is empty when neither was logged`() {
        val r = WeeklySummaryLogic.compute(mapOf(thisMon to DailyLog(waterMl = 500)), today)
        assertEquals("", r.moodEnergyLine)
    }

    @Test
    fun `a quieter week than last is never framed as a failure`() {
        // Four days last week, one this week.
        val logs = mapOf(thisMon to DailyLog(waterMl = 500)) +
            (0L until 4L).associate { lastMon.plusDays(it) to DailyLog(waterMl = 500) }
        val r = WeeklySummaryLogic.compute(logs, today)
        assertEquals(1, r.daysLogged)
        assertEquals(4, r.prevDaysLogged)
        listOf("should", "poor", "bad", "missed", "only", "fail", "less").forEach {
            assertFalse("copy must not scold: '$it' in \"${r.insight}\"", r.insight.lowercase().contains(it))
        }
    }

    @Test
    fun `momentum is named when she logged more than last week`() {
        val logs = (0L until 3L).associate { thisMon.plusDays(it) to DailyLog(waterMl = 500) } +
            mapOf(lastMon to DailyLog(waterMl = 500))
        val r = WeeklySummaryLogic.compute(logs, today)
        assertTrue(r.insight.contains("2 more than last week"))
    }
}
