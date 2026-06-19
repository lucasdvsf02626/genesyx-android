package com.genesyx.app.domain.cycle

import com.genesyx.app.domain.model.CalendarCell
import com.genesyx.app.domain.model.CycleSettings
import com.genesyx.app.domain.model.CyclePhaseInfo
import com.genesyx.app.domain.model.DayType
import com.genesyx.app.domain.model.FertileWindow
import com.genesyx.app.domain.model.Phase
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/**
 * Pure cycle math ported verbatim from web `src/lib/cycle.ts` + `src/lib/cycleEngine.ts`.
 * See docs/CYCLE_ENGINE.md. All dates are local (no UTC), matching the web's TZ-safe helpers.
 */
object CycleEngine {

    const val DEFAULT_CYCLE_LENGTH = 28
    const val DEFAULT_PERIOD_LENGTH = 5
    val CYCLE_LENGTH_RANGE = 21..35
    val PERIOD_LENGTH_RANGE = 1..10

    /** Whole-day difference (target - origin). */
    fun daysBetween(origin: LocalDate, target: LocalDate): Int =
        ChronoUnit.DAYS.between(origin, target).toInt()

    /**
     * Derived cycle state for [target].
     * dayOfCycle is 1-based and handles dates before the last period (negative diff).
     */
    fun getCyclePhase(
        lastPeriodDate: LocalDate,
        cycleLength: Int,
        periodLength: Int,
        target: LocalDate = LocalDate.now(),
    ): CyclePhaseInfo {
        val diff = daysBetween(lastPeriodDate, target)
        val dayOfCycle = ((diff % cycleLength) + cycleLength) % cycleLength + 1
        val ovulationDay = cycleLength - 14 // luteal phase fixed at 14 days
        val fertileWindow = FertileWindow(ovulationDay - 5, ovulationDay + 1)

        val phase = when {
            dayOfCycle <= periodLength -> Phase.PERIOD
            dayOfCycle == ovulationDay -> Phase.OVULATORY
            dayOfCycle < ovulationDay -> Phase.FOLLICULAR
            else -> Phase.LUTEAL
        }

        // Matches web cycle.ts: day 1 reports 0 (period just started).
        val daysUntilNextPeriod = if (dayOfCycle == 1) 0 else cycleLength - dayOfCycle

        return CyclePhaseInfo(
            dayOfCycle = dayOfCycle,
            phase = phase,
            fertileWindow = fertileWindow,
            ovulationDay = ovulationDay,
            daysUntilNextPeriod = daysUntilNextPeriod,
        )
    }

    fun getCyclePhase(settings: CycleSettings, target: LocalDate = LocalDate.now()): CyclePhaseInfo =
        getCyclePhase(settings.lastPeriodDate, settings.cycleLength, settings.periodLength, target)

    /** Calendar day classification. Order matches web cycle.ts: period > ovulation > fertile > luteal. */
    fun dayTypeFor(info: CyclePhaseInfo): DayType = when {
        info.phase == Phase.PERIOD -> DayType.PERIOD
        info.dayOfCycle == info.ovulationDay -> DayType.OVULATION
        info.dayOfCycle in info.fertileWindow -> DayType.FERTILE
        info.phase == Phase.LUTEAL -> DayType.LUTEAL
        else -> DayType.FOLLICULAR
    }

    /** 1-based cycle number for [target]. Dates before the last period clamp to cycle 1 (web parity). */
    fun cycleNumberFor(lastPeriodDate: LocalDate, cycleLength: Int, target: LocalDate = LocalDate.now()): Int =
        Math.floorDiv(daysBetween(lastPeriodDate, target).coerceAtLeast(0), cycleLength) + 1

    /** Sunday-first month grid with leading/trailing empty cells. */
    fun buildMonthGrid(
        monthAnchor: YearMonth,
        settings: CycleSettings,
        today: LocalDate = LocalDate.now(),
    ): List<CalendarCell> {
        val first = monthAnchor.atDay(1)
        val daysInMonth = monthAnchor.lengthOfMonth()
        val leading = first.dayOfWeek.value % 7 // ISO Mon=1..Sun=7 -> Sun=0..Sat=6

        val cells = ArrayList<CalendarCell>(42)
        repeat(leading) { cells.add(CalendarCell.Empty) }
        for (day in 1..daysInMonth) {
            val date = monthAnchor.atDay(day)
            cells.add(
                CalendarCell.Day(
                    date = date,
                    info = getCyclePhase(settings, date),
                    isToday = date == today,
                ),
            )
        }
        while (cells.size % 7 != 0) cells.add(CalendarCell.Empty)
        return cells
    }
}
