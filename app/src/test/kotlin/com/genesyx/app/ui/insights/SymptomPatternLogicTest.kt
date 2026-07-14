package com.genesyx.app.ui.insights

import com.genesyx.app.domain.model.DailyLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Validates the symptom heatmap and, above all, the thin-data guard that refuses to invent a pattern. */
class SymptomPatternLogicTest {

    private val today = LocalDate.of(2026, 6, 21)

    private fun log(vararg symptoms: String) = DailyLog(symptoms = symptoms.toSet())

    /** [days] consecutive days ending today, each holding [symptoms]. */
    private fun recentDays(days: Int, vararg symptoms: String) =
        (0 until days).associate { today.minusDays(it.toLong()) to log(*symptoms) }

    @Test
    fun `no symptoms logged is the empty state, and the grid is empty rather than absent`() {
        val r = SymptomPatternLogic.compute(emptyMap(), today)
        assertFalse(r.hasEnoughData)
        assertEquals(0, r.daysWithSymptoms)
        assertNull(r.topSymptom)
        assertEquals(28, r.heatmapValues.size)
        assertEquals(List(28) { 0 }, r.heatmapValues)
        assertEquals("No symptoms logged yet.", r.insight)
    }

    @Test
    fun `under seven symptom days it names no pattern`() {
        val r = SymptomPatternLogic.compute(recentDays(6, "Cramps"), today)
        assertFalse(r.hasEnoughData)
        assertEquals(6, r.daysWithSymptoms)
        assertTrue(r.insight.contains("Early days — too soon to read patterns."))
        // The top symptom is computed, but the copy must not present it as a finding yet.
        assertEquals("Cramps", r.topSymptom)
        assertFalse(r.insight.contains("Cramps"))
    }

    @Test
    fun `at seven symptom days it states the top symptom`() {
        val r = SymptomPatternLogic.compute(recentDays(7, "Cramps"), today)
        assertTrue(r.hasEnoughData)
        assertEquals(7, r.daysWithSymptoms)
        assertEquals("Cramps", r.topSymptom)
        assertEquals(7, r.topCount)
        assertTrue(r.insight.contains("Cramps is the symptom you log most"))
        assertTrue(r.insight.contains("7 days"))
    }

    @Test
    fun `a tie is broken alphabetically`() {
        // Seven days, each holding both. Equal counts — "Bloating" wins on the alphabet.
        val r = SymptomPatternLogic.compute(recentDays(7, "Cramps", "Bloating"), today)
        assertTrue(r.hasEnoughData)
        assertEquals("Bloating", r.topSymptom)
        assertEquals(7, r.topCount)
    }

    @Test
    fun `the most-logged symptom wins, not the most recent`() {
        val logs = buildMap {
            repeat(8) { put(today.minusDays(it.toLong()), log("Fatigue")) }
            repeat(3) { put(today.minusDays((20 + it).toLong()), log("Nausea")) }
        }
        val r = SymptomPatternLogic.compute(logs, today)
        assertEquals("Fatigue", r.topSymptom)
        assertEquals(8, r.topCount)
        assertEquals(11, r.daysWithSymptoms)
    }

    @Test
    fun `the heatmap runs oldest to newest, so today is the last cell`() {
        val logs = mapOf(
            today to log("Cramps", "Acne"), // 2 symptoms today
            today.minusDays(27) to log("Cramps"), // 1 symptom, the oldest day in the window
        )
        val r = SymptomPatternLogic.compute(logs, today)
        assertEquals("oldest day first", 1, r.heatmapValues.first())
        assertEquals("today last", 2, r.heatmapValues.last())
    }

    @Test
    fun `a day outside the 28-day window is not drawn, but still counts towards her history`() {
        val logs = mapOf(today.minusDays(40) to log("Cramps"))
        val r = SymptomPatternLogic.compute(logs, today)
        assertEquals("nothing to draw in the window", List(28) { 0 }, r.heatmapValues)
        assertEquals("but the day is real and is counted", 1, r.daysWithSymptoms)
    }

    @Test
    fun `the same symptom in two spellings is one symptom, counted once a day`() {
        // The Log offers a "Cramps" chip AND a free-text field, so both spellings can be stored.
        val logs = buildMap {
            repeat(4) { put(today.minusDays(it.toLong()), log("Cramps")) }
            repeat(3) { put(today.minusDays((4 + it).toLong()), log("cramps")) }
            // A single day holding both spellings is still one day of cramps.
            put(today.minusDays(7), log("Cramps", "cramps"))
        }
        val r = SymptomPatternLogic.compute(logs, today)
        assertEquals(8, r.daysWithSymptoms)
        assertEquals("Cramps", r.topSymptom) // alphabetically-first spelling of the group
        assertEquals("days, not string occurrences", 8, r.topCount)
    }

    @Test
    fun `a blank free-text entry is not a symptom`() {
        val r = SymptomPatternLogic.compute(mapOf(today to log("", "  ")), today)
        assertEquals(0, r.daysWithSymptoms)
        assertEquals(0, r.heatmapValues.last())
        assertNull(r.topSymptom)
    }
}
