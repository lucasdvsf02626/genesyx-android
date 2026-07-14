package com.genesyx.app.ui.insights

import com.genesyx.app.domain.model.CycleSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Validates the cycle-regularity card — above all, that it never claims a history the app does not
 * store. There is one cycle length in `cycle_settings` and nothing behind it.
 */
class CycleRegularityLogicTest {

    private fun settings(cycleLength: Int) =
        CycleSettings(lastPeriodDate = LocalDate.of(2026, 6, 1), cycleLength = cycleLength)

    @Test
    fun `no cycle settings is the empty state, and it prompts setup`() {
        val r = CycleRegularityLogic.compute(null)
        assertFalse(r.hasData)
        assertEquals(null, r.cycleLength)
        assertFalse(r.inTypicalRange)
        assertTrue(r.insight.contains("Set up your cycle"))
    }

    @Test
    fun `a cycle inside the typical range says so`() {
        val r = CycleRegularityLogic.compute(settings(28))
        assertTrue(r.hasData)
        assertEquals(28, r.cycleLength)
        assertTrue(r.inTypicalRange)
        assertTrue(r.insight.contains("28 days"))
        assertTrue(r.insight.contains("inside the typical 21–35 day range"))
    }

    @Test
    fun `the range boundaries are inside it, not outside`() {
        assertTrue(CycleRegularityLogic.compute(settings(21)).inTypicalRange)
        assertTrue(CycleRegularityLogic.compute(settings(35)).inTypicalRange)
    }

    @Test
    fun `a cycle outside the range points at a GP, and does not diagnose her`() {
        val r = CycleRegularityLogic.compute(settings(40))
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
        val r = CycleRegularityLogic.compute(settings(30))
        assertTrue(
            "the card must not imply a history the app does not store",
            r.insight.contains("not a measurement of past cycles"),
        )
    }

    @Test
    fun `the typical range is quoted from the engine, not restated`() {
        assertEquals(21, CycleRegularityLogic.typicalMin)
        assertEquals(35, CycleRegularityLogic.typicalMax)
    }
}
