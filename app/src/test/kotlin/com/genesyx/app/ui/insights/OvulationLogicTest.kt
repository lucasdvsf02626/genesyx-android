package com.genesyx.app.ui.insights

import com.genesyx.app.domain.model.CycleSettings
import com.genesyx.app.domain.model.DayType
import com.genesyx.app.domain.model.Phase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Validates the ovulation card. The branch tests matter, but the wording tests matter more: this
 * card predicts from a cycle length and a date, and it must never sound like it measured anything.
 */
class OvulationLogicTest {

    // A textbook 28-day cycle: ovulation on day 14, fertile window days 9–15, period days 1–5.
    private val settings = CycleSettings(
        lastPeriodDate = LocalDate.of(2026, 6, 1),
        cycleLength = 28,
        periodLength = 5,
    )

    private fun on(day: Int) = OvulationLogic.compute(settings, LocalDate.of(2026, 6, day))

    @Test
    fun `no cycle settings is the empty state`() {
        val r = OvulationLogic.compute(null)
        assertFalse(r.hasData)
        assertNull(r.ovulationDate)
        assertNull(r.fertileWindowStart)
        assertTrue(r.dayTypes.isEmpty())
        assertTrue(r.insight.contains("Set up your cycle"))
    }

    @Test
    fun `the predicted dates come off the cycle, and the ribbon covers every day of it`() {
        val r = on(10)
        assertTrue(r.hasData)
        assertEquals(28, r.cycleLength)
        assertEquals(10, r.currentDayOfCycle)
        assertEquals(14, r.ovulationDay)
        assertEquals(LocalDate.of(2026, 6, 14), r.ovulationDate)
        assertEquals(LocalDate.of(2026, 6, 9), r.fertileWindowStart)
        assertEquals(LocalDate.of(2026, 6, 15), r.fertileWindowEnd)
        assertEquals(28, r.dayTypes.size)
        assertEquals(DayType.OVULATION, r.dayTypes[13]) // day 14, 0-indexed
    }

    @Test
    fun `inside the fertile window the card says so, and calls it predicted`() {
        val r = on(10) // day 10, inside 9–15
        assertEquals(Phase.FOLLICULAR, r.currentPhase)
        assertEquals("Day 10 — you're in your predicted fertile window.", r.insight)
    }

    @Test
    fun `on ovulation day it predicts, and says outright that it is not a reading`() {
        val r = on(14)
        assertEquals(14, r.currentDayOfCycle)
        assertEquals(Phase.OVULATORY, r.currentPhase)
        assertTrue(r.insight.contains("today is your predicted ovulation day"))
        assertTrue(r.insight.contains("not a confirmed reading"))
    }

    @Test
    fun `before the fertile window it counts down the days`() {
        val r = on(7) // day 7 — follicular, not yet fertile
        assertEquals("Day 7 — 7 days until your predicted ovulation.", r.insight)
    }

    @Test
    fun `on her period it says so, and still gives the estimate`() {
        val r = on(2) // day 2 of a 5-day period
        assertEquals(Phase.PERIOD, r.currentPhase)
        assertTrue(r.insight.contains("on your period"))
        assertTrue(r.insight.contains("estimated in 12 days"))
    }

    @Test
    fun `in the luteal phase ovulation is estimated to have passed`() {
        val r = on(20)
        assertEquals(Phase.LUTEAL, r.currentPhase)
        assertTrue(r.insight.contains("luteal phase"))
        assertTrue(r.insight.contains("estimated to have passed"))
    }

    @Test
    fun `a period day that also falls inside the fertile window reads as a period day`() {
        // A 21-day cycle ovulates on day 7, so the fertile window is days 2–8. With a 10-day period,
        // day 4 is both. The Track calendar paints it as a period day; this card must agree with it.
        val short = CycleSettings(LocalDate.of(2026, 6, 1), cycleLength = 21, periodLength = 10)
        val r = OvulationLogic.compute(short, LocalDate.of(2026, 6, 4))
        assertEquals(4, r.currentDayOfCycle)
        assertTrue(r.insight.contains("on your period"))
        assertFalse(r.insight.contains("fertile window"))
    }

    @Test
    fun `every branch predicts and none of them confirms`() {
        listOf(2, 7, 10, 14, 20).forEach { day ->
            val insight = on(day).insight
            assertTrue(
                "must hedge: \"$insight\"",
                insight.contains("predicted") || insight.contains("estimated"),
            )
            listOf("you are ovulating", "confirmed ovulation", "you ovulated").forEach { claim ->
                assertFalse("must not claim '$claim': \"$insight\"", insight.lowercase().contains(claim))
            }
        }
    }
}
