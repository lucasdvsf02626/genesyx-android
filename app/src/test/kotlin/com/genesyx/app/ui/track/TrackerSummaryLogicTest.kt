package com.genesyx.app.ui.track

import com.genesyx.app.domain.model.CycleSettings
import com.genesyx.app.domain.model.DailyLog
import com.genesyx.app.domain.model.PhMeasurement
import com.genesyx.app.domain.model.PhReading
import com.genesyx.app.domain.model.Supplement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class TrackerSummaryLogicTest {

    private val today = LocalDate.of(2026, 6, 21)
    private val goal = 2400

    @Test
    fun `no data gives honest empty summaries, never a fake zero or healthy status`() {
        val s = TrackerSummaryLogic.compute(emptyMap(), emptyList(), settings = null, goalMl = goal, today = today)
        assertFalse(s.cycle.hasData)
        assertFalse(s.hydration.hasData)
        assertFalse(s.ph.hasData)
        assertFalse(s.sleep.hasData)
        assertFalse(s.symptoms.hasData)
        assertFalse(s.nutrition.hasData)
        assertEquals("No readings yet — log your first", s.ph.value)
        assertEquals("No water logged today", s.hydration.value)
        // Empty spark rows are all-false, never seeded.
        assertTrue(s.hydration.spark.all { !it })
    }

    @Test
    fun `cycle summary reads day and phase from settings`() {
        val settings = CycleSettings(lastPeriodDate = today.minusDays(8), cycleLength = 28, periodLength = 5)
        val s = TrackerSummaryLogic.compute(emptyMap(), emptyList(), settings, goal, today = today)
        assertTrue(s.cycle.hasData)
        assertTrue(s.cycle.value.startsWith("Day 9 · "))
    }

    @Test
    fun `hydration summary reflects today's real total`() {
        val logs = mapOf(today to DailyLog(waterMl = 1600))
        val s = TrackerSummaryLogic.compute(logs, emptyList(), null, goal, today = today)
        assertTrue(s.hydration.hasData)
        assertEquals("1.6 L of 2.4 L today", s.hydration.value)
        assertTrue(s.hydration.spark.last()) // today (newest) is on
    }

    @Test
    fun `ph summary shows the latest vaginal reading and status`() {
        val readings = listOf(
            PhReading(phValue = 4.0, recordedAt = LocalDateTime.of(2026, 6, 19, 9, 0)),
            PhReading(phValue = 4.8, recordedAt = LocalDateTime.of(2026, 6, 21, 8, 0)),
        )
        val s = TrackerSummaryLogic.compute(emptyMap(), readings, null, goal, today = today)
        assertTrue(s.ph.hasData)
        assertEquals("Last reading 4.8 · Elevated", s.ph.value)
    }

    @Test
    fun `ph summary marks a legacy urine reading rather than classifying it`() {
        val readings = listOf(
            PhReading(
                phValue = 6.5,
                recordedAt = LocalDateTime.of(2026, 6, 21, 8, 0),
                measurementType = PhMeasurement.URINE,
            ),
        )
        val s = TrackerSummaryLogic.compute(emptyMap(), readings, null, goal, today = today)
        assertTrue(s.ph.hasData)
        assertEquals("Last reading 6.5 · urine (legacy)", s.ph.value)
    }

    @Test
    fun `sleep summary uses the most recent logged night, not an unlogged zero`() {
        val logs = mapOf(today.minusDays(1) to DailyLog(sleepMinutes = 440))
        val s = TrackerSummaryLogic.compute(logs, emptyList(), null, goal, today = today)
        assertTrue(s.sleep.hasData)
        assertTrue(s.sleep.value.contains("7h 20m"))
    }

    @Test
    fun `nutrition summary counts plan supplements taken today`() {
        val logs = mapOf(today to DailyLog(supplements = setOf(Supplement.FOLATE.wireName, Supplement.ZINC.wireName)))
        val s = TrackerSummaryLogic.compute(logs, emptyList(), null, goal, today = today)
        assertTrue(s.nutrition.hasData)
        assertEquals("2 of ${Supplement.defaultPlan.size} supplements today", s.nutrition.value)
    }

    @Test
    fun `symptoms summary counts the week's logged symptoms`() {
        val logs = mapOf(
            today to DailyLog(symptoms = setOf("Cramps", "Bloating")),
            today.minusDays(2) to DailyLog(symptoms = setOf("Headache")),
        )
        val s = TrackerSummaryLogic.compute(logs, emptyList(), null, goal, today = today)
        assertTrue(s.symptoms.hasData)
        assertEquals("3 symptoms logged this week", s.symptoms.value)
    }
}
