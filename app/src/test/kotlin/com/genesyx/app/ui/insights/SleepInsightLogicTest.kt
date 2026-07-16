package com.genesyx.app.ui.insights

import com.genesyx.app.domain.model.DailyLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Validates the sleep card: the 10-hour bar ceiling, and the average that ignores unlogged nights. */
class SleepInsightLogicTest {

    // Sunday — the current Mon–Sun week is 2026-06-15..21.
    private val today = LocalDate.of(2026, 6, 21)
    private val monday = LocalDate.of(2026, 6, 15)

    private fun night(minutes: Int) = DailyLog(sleepMinutes = minutes)

    @Test
    fun `no sleep logged this week is the empty state`() {
        val r = SleepInsightLogic.compute(emptyMap(), today)
        assertFalse(r.hasData)
        assertEquals(0, r.nightsLogged)
        assertEquals(null, r.nightlyAverageMinutes)
        assertEquals(List(7) { 0 }, r.bars)
        assertEquals(SleepInsights().insight, r.insight)
    }

    @Test
    fun `the average divides by nights logged, not by seven`() {
        // Two nights of eight hours. The average is 8h — NOT 16h/7 = 2h17m.
        val r = SleepInsightLogic.compute(
            mapOf(monday to night(480), monday.plusDays(1) to night(480)),
            today,
        )
        assertTrue(r.hasData)
        assertEquals(2, r.nightsLogged)
        assertEquals(480, r.nightlyAverageMinutes)
        assertEquals("8h", SleepInsightLogic.formatDuration(r.nightlyAverageMinutes!!))
    }

    @Test
    fun `a null night is not a night of no sleep, and neither is a stored zero`() {
        val logs = mapOf(
            monday to night(420),
            monday.plusDays(1) to DailyLog(sleepMinutes = null), // logged her water, not her sleep
            monday.plusDays(2) to night(0), // a stored zero says the same thing a null does
        )
        val r = SleepInsightLogic.compute(logs, today)
        assertEquals("only the real night counts", 1, r.nightsLogged)
        assertEquals(420, r.nightlyAverageMinutes)
        // The unlogged nights still draw a flat track, they just do not drag the average down.
        assertEquals(0, r.bars[1])
        assertEquals(0, r.bars[2])
    }

    @Test
    fun `bars scale against the ten-hour ceiling and never overflow it`() {
        val logs = mapOf(
            monday to night(300), // 5h  -> 50
            monday.plusDays(1) to night(600), // 10h -> 100
            monday.plusDays(2) to night(720), // 12h -> clamped to 100, not 120
        )
        val r = SleepInsightLogic.compute(logs, today)
        assertEquals(listOf(50, 100, 100, 0, 0, 0, 0), r.bars)
    }

    @Test
    fun `only the current week counts`() {
        val lastWeek = mapOf(monday.minusDays(1) to night(480)) // the Sunday before
        assertFalse(SleepInsightLogic.compute(lastWeek, today).hasData)
    }

    @Test
    fun `the copy reports the number and does not scold her for a short night`() {
        val r = SleepInsightLogic.compute(mapOf(monday to night(240)), today) // 4h
        assertTrue(r.insight.contains("4h"))
        listOf("should", "poor", "bad", "not enough", "too little").forEach {
            assertFalse("copy must not judge: '$it' in \"${r.insight}\"", r.insight.lowercase().contains(it))
        }
    }

    @Test
    fun `durations read as hours and minutes, never as a decimal`() {
        assertEquals("7h 20m", SleepInsightLogic.formatDuration(440))
        assertEquals("8h", SleepInsightLogic.formatDuration(480))
        assertEquals("45m", SleepInsightLogic.formatDuration(45))
    }
}
