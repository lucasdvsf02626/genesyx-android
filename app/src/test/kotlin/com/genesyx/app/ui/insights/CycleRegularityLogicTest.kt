package com.genesyx.app.ui.insights

import com.genesyx.app.domain.cycle.CycleEngine
import com.genesyx.app.domain.model.CycleSettings
import com.genesyx.app.domain.model.Phase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Validates the cycle-phase-timeline card — above all, that it never claims a history the app does
 * not store (one cycle length in `cycle_settings`, nothing behind it), and that the timeline's
 * segments can never disagree with [CycleEngine], the one source of phase truth.
 */
class CycleRegularityLogicTest {

    private val lastPeriod = LocalDate.of(2026, 6, 1)

    private fun settings(cycleLength: Int, periodLength: Int = 5) =
        CycleSettings(lastPeriodDate = lastPeriod, cycleLength = cycleLength, periodLength = periodLength)

    /** `today` landing on the given 1-based day of the first cycle. */
    private fun dayOf(day: Int): LocalDate = lastPeriod.plusDays((day - 1).toLong())

    /** Cycle/period pairs spanning the allowed ranges, including the degenerate long-period case. */
    private val configs = listOf(21 to 5, 28 to 5, 30 to 6, 35 to 10, 21 to 10)

    @Test
    fun `no cycle settings is the empty state, and it prompts setup`() {
        val r = CycleRegularityLogic.compute(null, today = dayOf(10))
        assertFalse(r.hasData)
        assertEquals(null, r.cycleLength)
        assertFalse(r.inTypicalRange)
        assertTrue(r.insight.contains("Set up your cycle"))
    }

    @Test
    fun `a cycle inside the typical range fills the timeline state`() {
        val r = CycleRegularityLogic.compute(settings(28), today = dayOf(10))
        assertTrue(r.hasData)
        assertEquals(28, r.cycleLength)
        assertTrue(r.inTypicalRange)
        assertEquals(10, r.dayOfCycle)
        assertEquals(Phase.FOLLICULAR, r.currentPhase)
        assertEquals(CycleRegularityLogic.phaseInsight.getValue(Phase.FOLLICULAR), r.phaseInsightLine)
    }

    @Test
    fun `the range boundaries are inside it, not outside`() {
        assertTrue(CycleRegularityLogic.compute(settings(21), today = dayOf(1)).inTypicalRange)
        assertTrue(CycleRegularityLogic.compute(settings(35), today = dayOf(1)).inTypicalRange)
    }

    @Test
    fun `a cycle outside the range points at a GP, and does not diagnose her`() {
        val r = CycleRegularityLogic.compute(settings(40), today = dayOf(10))
        assertTrue(r.hasData)
        assertFalse(r.inTypicalRange)
        assertTrue(r.insight.contains("outside the typical"))
        assertTrue(r.insight.contains("GP"))
        // It must not put a name to it, or call her cycle irregular on this evidence.
        listOf("pcos", "irregular", "abnormal", "disorder", "condition").forEach {
            assertFalse("must not diagnose: '$it' in \"${r.insight}\"", r.insight.lowercase().contains(it))
        }
    }

    @Test
    fun `the copy is honest that this is her setup, not a measurement of past cycles`() {
        val inRange = CycleRegularityLogic.compute(settings(30), today = dayOf(10))
        val outOfRange = CycleRegularityLogic.compute(settings(40), today = dayOf(10))
        listOf(inRange, outOfRange).forEach { r ->
            assertTrue(
                "the card must not imply a history the app does not store",
                r.insight.contains("not a measurement of past cycles"),
            )
        }
    }

    @Test
    fun `the typical range is quoted from the engine, not restated`() {
        assertEquals(21, CycleRegularityLogic.typicalMin)
        assertEquals(35, CycleRegularityLogic.typicalMax)
    }

    @Test
    fun `segments tile the cycle with no gaps or overlaps`() {
        configs.forEach { (cycle, period) ->
            val segs = CycleRegularityLogic.segments(settings(cycle, period))
            assertEquals("($cycle,$period) starts at day 1", 1, segs.first().startDay)
            assertEquals("($cycle,$period) ends at day $cycle", cycle, segs.last().endDay)
            segs.zipWithNext().forEach { (a, b) ->
                assertEquals("($cycle,$period) contiguous at ${a.endDay}", a.endDay + 1, b.startDay)
            }
        }
    }

    @Test
    fun `every day's segment phase agrees with the engine`() {
        configs.forEach { (cycle, period) ->
            val s = settings(cycle, period)
            val segs = CycleRegularityLogic.segments(s)
            for (day in 1..cycle) {
                val segment = segs.first { day in it }
                val engine = CycleEngine.getCyclePhase(s, dayOf(day)).phase
                assertEquals("($cycle,$period) day $day", engine, segment.phase)
            }
        }
    }

    @Test
    fun `the ovulatory segment is a single day when it exists, widened only visually`() {
        configs.forEach { (cycle, period) ->
            val segs = CycleRegularityLogic.segments(settings(cycle, period))
            val ovulatory = segs.filter { it.phase == Phase.OVULATORY }
            ovulatory.forEach { seg ->
                assertEquals("($cycle,$period) ovulatory is the engine's single day", 1, seg.dayCount)
                assertEquals("($cycle,$period) ovulatory sits at cycle − 14", cycle - 14, seg.startDay)
            }
            // The widening lives in the weights, never in the day ranges.
            val weights = CycleRegularityLogic.visualWeights(segs)
            segs.forEachIndexed { i, seg ->
                assertEquals(
                    maxOf(seg.dayCount.toFloat(), CycleRegularityLogic.MIN_VISUAL_DAYS),
                    weights[i],
                )
            }
        }
    }

    @Test
    fun `a long period in a short cycle swallows ovulation — two segments, not four`() {
        // ovulationDay = 21 − 14 = 7 falls inside a 10-day period, so the engine never says
        // FOLLICULAR or OVULATORY. The card must render the list it gets.
        val segs = CycleRegularityLogic.segments(settings(21, periodLength = 10))
        assertEquals(listOf(Phase.PERIOD, Phase.LUTEAL), segs.map { it.phase })

        val r = CycleRegularityLogic.compute(settings(21, periodLength = 10), today = dayOf(5))
        assertEquals(Phase.PERIOD, r.currentPhase)
        assertEquals(Phase.LUTEAL, r.nextPhase)
        assertEquals(6, r.daysUntilNextPhase)
    }

    @Test
    fun `next phase counts down to the following segment`() {
        // Day 13 of 28: follicular, one day short of ovulation day 14.
        val r = CycleRegularityLogic.compute(settings(28), today = dayOf(13))
        assertEquals(Phase.OVULATORY, r.nextPhase)
        assertEquals(1, r.daysUntilNextPhase)
    }

    @Test
    fun `the luteal phase wraps to the next period, and never says in 0 days`() {
        val mid = CycleRegularityLogic.compute(settings(28), today = dayOf(20))
        assertEquals(Phase.LUTEAL, mid.currentPhase)
        assertEquals(Phase.PERIOD, mid.nextPhase)
        assertEquals(9, mid.daysUntilNextPhase)

        // The last day of the cycle: the next period starts tomorrow, and 1 is the floor.
        val last = CycleRegularityLogic.compute(settings(28), today = dayOf(28))
        assertEquals(Phase.PERIOD, last.nextPhase)
        assertEquals(1, last.daysUntilNextPhase)
    }

    @Test
    fun `the marker always lands inside its own segment's visual span`() {
        configs.forEach { (cycle, period) ->
            val segs = CycleRegularityLogic.segments(settings(cycle, period))
            val weights = CycleRegularityLogic.visualWeights(segs)
            val total = weights.sum()
            for (day in 1..cycle) {
                val fraction = CycleRegularityLogic.markerFraction(segs, day)
                assertTrue("($cycle,$period) day $day above 0", fraction > 0f)
                assertTrue("($cycle,$period) day $day below 1", fraction < 1f)

                val index = segs.indexOfFirst { day in it }
                val before = weights.take(index).sum()
                assertTrue("($cycle,$period) day $day past its segment's start", fraction > before / total)
                assertTrue(
                    "($cycle,$period) day $day before its segment's end",
                    fraction < (before + weights[index]) / total,
                )
            }
        }
    }

    @Test
    fun `the first phase is labelled Period, not Menstrual`() {
        assertEquals("Period", CycleRegularityLogic.shortPhaseLabel.getValue(Phase.PERIOD))
        assertEquals("Follicular", CycleRegularityLogic.shortPhaseLabel.getValue(Phase.FOLLICULAR))
        assertEquals("Ovulatory", CycleRegularityLogic.shortPhaseLabel.getValue(Phase.OVULATORY))
        assertEquals("Luteal", CycleRegularityLogic.shortPhaseLabel.getValue(Phase.LUTEAL))
    }

    @Test
    fun `phase copy and the disclaimer stay wellness-safe`() {
        // Pinned verbatim, deliberately not read from a shared constant elsewhere in the test.
        assertEquals(
            "Phase dates are estimates based on your logged cycle information.",
            CycleRegularityLogic.DISCLAIMER,
        )

        val banned = listOf(
            "pcos", "irregular", "abnormal", "disorder", "condition",
            "fertile", "fertility", "conception", "pregnan", "diagnos",
        )
        val allCopy = CycleRegularityLogic.phaseInsight.values + CycleRegularityLogic.DISCLAIMER
        assertEquals(Phase.entries.size, CycleRegularityLogic.phaseInsight.size)
        allCopy.forEach { line ->
            banned.forEach { word ->
                assertFalse("banned '$word' in \"$line\"", line.lowercase().contains(word))
            }
        }
    }
}
