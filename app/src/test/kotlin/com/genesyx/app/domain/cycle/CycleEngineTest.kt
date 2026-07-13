package com.genesyx.app.domain.cycle

import com.genesyx.app.domain.model.CalendarCell
import com.genesyx.app.domain.model.CycleSettings
import com.genesyx.app.domain.model.DayType
import com.genesyx.app.domain.model.Phase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

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
    fun `fertile window is inclusive at both boundaries`() {
        val info = phaseOn(lastPeriod)
        assertTrue("start day 9 is fertile", 9 in info.fertileWindow)
        assertTrue("end day 15 is fertile", 15 in info.fertileWindow)
        assertFalse("day 8 is not fertile", 8 in info.fertileWindow)
        assertFalse("day 16 is not fertile", 16 in info.fertileWindow)
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

    @Test
    fun `daysUntilNextPeriod is zero on day one and counts down otherwise`() {
        // Matches web cycle.ts: day 1 reports 0.
        assertEquals(0, phaseOn(lastPeriod).daysUntilNextPeriod)
        assertEquals(20, phaseOn(lastPeriod.plusDays(7)).daysUntilNextPeriod) // day 8 -> 28-8
        assertEquals(1, phaseOn(lastPeriod.plusDays(26)).daysUntilNextPeriod) // day 27 -> 28-27
    }

    @Test
    fun `period takes precedence over fertile when they overlap on a short cycle`() {
        // 21-day cycle: ovulation = day 7, fertile window = [2, 8]; period = days 1-5.
        // Days 2-5 are both period and fertile — web cycle.ts classifies them as period.
        val shortLast = LocalDate.of(2026, 6, 1)
        val day3 = CycleEngine.getCyclePhase(shortLast, 21, 5, shortLast.plusDays(2)) // day 3
        assertEquals(Phase.PERIOD, day3.phase)
        assertTrue(3 in day3.fertileWindow)
        assertEquals("period wins over fertile", DayType.PERIOD, CycleEngine.dayTypeFor(day3))

        // Day 6 is fertile but no longer period -> fertile.
        val day6 = CycleEngine.getCyclePhase(shortLast, 21, 5, shortLast.plusDays(5))
        assertEquals(DayType.FERTILE, CycleEngine.dayTypeFor(day6))
    }

    @Test
    fun `luteal day after the fertile window is classified luteal`() {
        val info = phaseOn(lastPeriod.plusDays(19)) // day 20, luteal, outside fertile
        assertEquals(Phase.LUTEAL, info.phase)
        assertEquals(DayType.LUTEAL, CycleEngine.dayTypeFor(info))
    }

    @Test
    fun `cycle number increments once per full cycle`() {
        assertEquals(1, CycleEngine.cycleNumberFor(lastPeriod, cycleLength, lastPeriod))
        assertEquals(2, CycleEngine.cycleNumberFor(lastPeriod, cycleLength, lastPeriod.plusDays(28)))
        assertEquals(3, CycleEngine.cycleNumberFor(lastPeriod, cycleLength, lastPeriod.plusDays(56)))
        // Dates before the last period clamp to cycle 1.
        assertEquals(1, CycleEngine.cycleNumberFor(lastPeriod, cycleLength, lastPeriod.minusDays(3)))
    }

    @Test
    fun `month grid is sunday-first, padded to full weeks, with correct day count and today flag`() {
        val settings = CycleSettings(lastPeriod, cycleLength, periodLength)
        val anchor = YearMonth.of(2026, 6) // June 2026: the 1st is a Monday
        val today = LocalDate.of(2026, 6, 15)
        val cells = CycleEngine.buildMonthGrid(anchor, settings, today)

        assertEquals("grid is whole weeks", 0, cells.size % 7)
        val dayCells = cells.filterIsInstance<CalendarCell.Day>()
        assertEquals("30 day cells for June", 30, dayCells.size)
        // Monday -> one leading empty (Sunday-first).
        assertTrue(cells.first() is CalendarCell.Empty)
        assertEquals(1, cells.indexOfFirst { it is CalendarCell.Day })
        assertEquals(1, dayCells.count { it.isToday })
        assertTrue(dayCells.single { it.isToday }.date == today)
    }

    // ── calendar bounds ──
    //
    // getCyclePhase's modulo happily projects backwards into months before the last recorded period
    // and forwards forever. The calendar used to let the user page through all of it, painting
    // period and ovulation days as confidently as real ones. These pin where it now stops.

    @Test
    fun `the calendar starts at the month of the last recorded period`() {
        val settings = CycleSettings(LocalDate.of(2026, 6, 10), cycleLength, periodLength)
        assertEquals(YearMonth.of(2026, 6), CycleEngine.earliestMonth(settings))
    }

    @Test
    fun `the calendar stops a few cycles ahead of today`() {
        val today = LocalDate.of(2026, 6, 15)
        assertEquals(
            YearMonth.of(2026, 6).plusMonths(CycleEngine.FORECAST_MONTHS),
            CycleEngine.latestMonth(today),
        )
    }

    @Test
    fun `a month before the last period clamps forward to it`() {
        val settings = CycleSettings(LocalDate.of(2026, 6, 10), cycleLength, periodLength)
        val today = LocalDate.of(2026, 6, 15)
        // There is no basis whatsoever for January — it must not be reachable.
        val clamped = CycleEngine.clampMonth(YearMonth.of(2026, 1), settings, today)
        assertEquals(YearMonth.of(2026, 6), clamped)
    }

    @Test
    fun `a month beyond the forecast horizon clamps back to it`() {
        val settings = CycleSettings(LocalDate.of(2026, 6, 10), cycleLength, periodLength)
        val today = LocalDate.of(2026, 6, 15)
        val clamped = CycleEngine.clampMonth(YearMonth.of(2099, 1), settings, today)
        assertEquals(CycleEngine.latestMonth(today), clamped)
    }

    @Test
    fun `a month inside the range is left alone`() {
        val settings = CycleSettings(LocalDate.of(2026, 6, 10), cycleLength, periodLength)
        val today = LocalDate.of(2026, 6, 15)
        assertEquals(
            YearMonth.of(2026, 7),
            CycleEngine.clampMonth(YearMonth.of(2026, 7), settings, today),
        )
    }
}
