package com.genesyx.app.ui.track.detail

import com.genesyx.app.domain.model.DailyLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** The Track → Nutrition summary: today's names and the Mon–Sun dots, read-only. */
class NutritionTrackerLogicTest {

    // Sunday — the current Mon–Sun week is 2026-06-15..21.
    private val today = LocalDate.of(2026, 6, 21)
    private val monday = LocalDate.of(2026, 6, 15)

    @Test
    fun `nothing logged is the empty state, and the footer shows`() {
        val r = NutritionTrackerLogic.compute(emptyMap(), today)
        assertTrue(r.todaySupplements.isEmpty())
        assertTrue(r.todayFoodGroups.isEmpty())
        assertEquals(List(7) { 0 }, r.weekSupplementCounts)
        assertEquals(List(7) { 0 }, r.weekFoodGroupCounts)
        assertTrue(r.supplementWeekEmpty)
    }

    @Test
    fun `today lists display names for known wire names and keeps unknown strings verbatim`() {
        val logs = mapOf(today to DailyLog(supplements = setOf("Folic acid", " zinc ", "Magnesium", "")))
        val r = NutritionTrackerLogic.compute(logs, today)
        assertEquals(setOf("Folate", "Zinc", "Magnesium"), r.todaySupplements.toSet())
    }

    @Test
    fun `food groups read as labels`() {
        val logs = mapOf(today to DailyLog(foodGroups = setOf("vegetables", "oilsAndFats", "somethingNew")))
        val r = NutritionTrackerLogic.compute(logs, today)
        assertEquals(setOf("Vegetables", "Oils & fats", "somethingNew"), r.todayFoodGroups.toSet())
    }

    @Test
    fun `the week strips count per day and one logged day hides the footer`() {
        val logs = mapOf(
            monday to DailyLog(supplements = setOf("Folic acid", "Zinc"), foodGroups = setOf("fruit")),
            monday.plusDays(2) to DailyLog(supplements = setOf("Iron")),
            monday.minusDays(1) to DailyLog(supplements = setOf("Folic acid")), // last week — ignored
        )
        val r = NutritionTrackerLogic.compute(logs, today)
        assertEquals(listOf(2, 0, 1, 0, 0, 0, 0), r.weekSupplementCounts)
        assertEquals(listOf(1, 0, 0, 0, 0, 0, 0), r.weekFoodGroupCounts)
        assertFalse(r.supplementWeekEmpty)
    }
}
