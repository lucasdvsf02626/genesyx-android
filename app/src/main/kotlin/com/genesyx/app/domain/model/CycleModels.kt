package com.genesyx.app.domain.model

import java.time.LocalDate

/** Cycle phase. Mirrors `Phase` in web `src/lib/cycle.ts`. */
enum class Phase { PERIOD, FOLLICULAR, OVULATORY, LUTEAL }

/** Calendar day classification (Phase + fertile/ovulation overlays). */
enum class DayType { PERIOD, FOLLICULAR, FERTILE, OVULATION, LUTEAL }

/** Inclusive day-of-cycle range for the fertile window. */
data class FertileWindow(val startDay: Int, val endDay: Int) {
    operator fun contains(dayOfCycle: Int) = dayOfCycle in startDay..endDay
}

/** Derived cycle state for a target date. */
data class CyclePhaseInfo(
    val dayOfCycle: Int,
    val phase: Phase,
    val fertileWindow: FertileWindow,
    val ovulationDay: Int,
    val daysUntilNextPeriod: Int,
)

/** A single calendar cell in the month grid. */
sealed interface CalendarCell {
    data object Empty : CalendarCell
    data class Day(
        val date: LocalDate,
        val info: CyclePhaseInfo,
        val isToday: Boolean,
    ) : CalendarCell
}

/** User cycle configuration (`cycle_settings`). */
data class CycleSettings(
    val lastPeriodDate: LocalDate,
    val cycleLength: Int = 28,
    val periodLength: Int = 5,
)
