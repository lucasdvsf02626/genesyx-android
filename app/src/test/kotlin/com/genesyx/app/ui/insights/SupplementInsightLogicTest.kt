package com.genesyx.app.ui.insights

import com.genesyx.app.domain.model.DailyLog
import com.genesyx.app.domain.model.Supplement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Validates the supplement-adherence card: plan-based bars, the tiles, and the copy's manners. */
class SupplementInsightLogicTest {

    // Sunday — the current Mon–Sun week is 2026-06-15..21.
    private val today = LocalDate.of(2026, 6, 21)
    private val monday = LocalDate.of(2026, 6, 15)

    /** A log holding exactly [supplements], by their stored (wire) names. */
    private fun log(vararg supplements: Supplement) =
        DailyLog(supplements = supplements.map { it.wireName }.toSet())

    private val wholePlan = Supplement.defaultPlan.toTypedArray()

    @Test
    fun `nothing logged this week is the empty state, and it does not scold`() {
        val r = SupplementInsightLogic.compute(emptyMap(), today)
        assertFalse(r.hasData)
        assertTrue(r.hasPlan)
        assertEquals(0, r.daysLogged)
        assertEquals(0, r.suppTotal)
        assertEquals(SupplementInsights().insight, r.insight)
        assertTrue(r.insight.contains("gentle start"))
    }

    @Test
    fun `a full plan on a day is a full bar`() {
        val r = SupplementInsightLogic.compute(mapOf(monday to log(*wholePlan)), today)
        assertEquals(listOf(100, 0, 0, 0, 0, 0, 0), r.bars)
        assertEquals(1, r.daysLogged)
        assertEquals(4, r.suppTotal)
        assertEquals(4, r.planSize)
    }

    @Test
    fun `a partial day scores against the plan, not the number logged`() {
        // Two of a four-item plan.
        val r = SupplementInsightLogic.compute(
            mapOf(monday to log(Supplement.FOLATE, Supplement.ZINC)),
            today,
        )
        assertEquals(listOf(50, 0, 0, 0, 0, 0, 0), r.bars)
        assertEquals(2, r.suppTotal)
    }

    @Test
    fun `iron is recorded but never scored, and cannot push a bar past full`() {
        // The whole plan PLUS iron. Iron is outside the plan, so the day is 100 — not 125.
        val r = SupplementInsightLogic.compute(
            mapOf(monday to log(*wholePlan, Supplement.IRON)),
            today,
        )
        assertEquals(100, r.bars.first())
        assertEquals("iron must not count towards adherence", 4, r.suppTotal)

        // And iron ALONE is a logged day that scores zero — recorded, not adherence.
        val ironOnly = SupplementInsightLogic.compute(mapOf(monday to log(Supplement.IRON)), today)
        assertFalse("a day of iron alone is not plan adherence", ironOnly.hasData)
    }

    @Test
    fun `a full week reads as steady, and names no day she missed`() {
        val week = (0L until 7L).associate { monday.plusDays(it) to log(*wholePlan) }
        val r = SupplementInsightLogic.compute(week, today)
        assertEquals(List(7) { 100 }, r.bars)
        assertEquals(7, r.daysLogged)
        assertEquals(28, r.suppTotal)
        assertTrue(r.insight.contains("Beautifully steady"))
        listOf("missed", "haven't", "lost", "failed", "forgot").forEach {
            assertFalse("copy must not scold: '$it' in \"${r.insight}\"", r.insight.lowercase().contains(it))
        }
    }

    @Test
    fun `a single logged day reads as early days, not as six missed ones`() {
        val r = SupplementInsightLogic.compute(mapOf(monday to log(Supplement.FOLATE)), today)
        assertTrue(r.hasData)
        assertEquals(1, r.daysLogged)
        assertTrue(r.insight.contains("Early days"))
    }

    @Test
    fun `only the current week counts`() {
        val lastWeek = mapOf(monday.minusDays(1) to log(*wholePlan)) // the Sunday before
        assertFalse(SupplementInsightLogic.compute(lastWeek, today).hasData)
    }

    @Test
    fun `an unrecognised stored string does not score and does not crash`() {
        // What an older build, or another client, might have written.
        val odd = mapOf(monday to DailyLog(supplements = setOf("Folate", "Magnesium", "")))
        val r = SupplementInsightLogic.compute(odd, today)
        assertFalse(r.hasData)
        assertEquals(0, r.suppTotal)
    }

    @Test
    fun `an empty plan invites her to set one rather than showing zero out of zero`() {
        val r = SupplementInsightLogic.compute(mapOf(monday to log(*wholePlan)), today, plan = emptyList())
        assertFalse(r.hasPlan)
        assertFalse(r.hasData)
    }
}
