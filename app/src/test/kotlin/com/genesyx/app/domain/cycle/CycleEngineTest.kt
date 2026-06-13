package com.genesyx.app.domain.cycle

import com.genesyx.app.domain.model.DayType
import com.genesyx.app.domain.model.Phase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Validates the cycle engine against the web `cycle.ts` formulas (docs/CYCLE_ENGINE.md). */
class CycleEngineTest {

    private val lastPeriod = LocalDate.of(2026, 6, 1)
    private val cycleLength = 28
    private val periodLength = 5

    private fun phaseOn(date: LocalDate) =
        CycleEngine.getCyclePhase(lastPeriod, cycleLength, periodLength, date)

    @Test
    fun `day one is period, day of cycle 1`() {
        val info = phaseOn(lastPeriod)
        assertEquals(1, info.dayOfCycle)
        assertEquals(Phase.PERIOD, info.phase)
    }

    @Test
    fun `ovulation day equals cycleLength minus 14`() {
        val ovDate = lastPeriod.plusDays(13) // day 14 for a 28-day cycle
        val info = phaseOn(ovDate)
        assertEquals(14, info.dayOfCycle)
        assertEquals(14, info.ovulationDay)
        assertEquals(Phase.OVULATORY, info.phase)
        assertEquals(DayType.OVULATION, CycleEngine.dayTypeFor(info))
    }

    @Test
    fun `fertile window spans ovulation minus 5 to plus 1`() {
        val info = phaseOn(lastPeriod)
        assertEquals(9, info.fertileWindow.startDay)
        assertEquals(15, info.fertileWindow.endDay)
        assertTrue(12 in info.fertileWindow)
        assertTrue(16 !in info.fertileWindow)
    }

    @Test
    fun `follicular before ovulation, luteal after`() {
        assertEquals(Phase.FOLLICULAR, phaseOn(lastPeriod.plusDays(7)).phase) // day 8
        assertEquals(Phase.LUTEAL, phaseOn(lastPeriod.plusDays(20)).phase) // day 21
    }

    @Test
    fun `day of cycle wraps across cycles and handles dates before last period`() {
        // 28 days later = start of next cycle, day 1 again
        assertEquals(1, phaseOn(lastPeriod.plusDays(28)).dayOfCycle)
        // one day before last period = last day of previous cycle (day 28)
        assertEquals(28, phaseOn(lastPeriod.minusDays(1)).dayOfCycle)
    }

    @Test
    fun `fertile day is classified as fertile not phase`() {
        val info = phaseOn(lastPeriod.plusDays(11)) // day 12, within [9,15], not ovulation
        assertEquals(DayType.FERTILE, CycleEngine.dayTypeFor(info))
    }
}
