package com.genesyx.app.ui.insights

import com.genesyx.app.domain.cycle.CycleEngine
import com.genesyx.app.domain.model.CycleSettings
import com.genesyx.app.domain.model.DayType
import com.genesyx.app.domain.model.Phase
import java.time.LocalDate

/**
 * Pure ovulation computation. All cycle maths comes from [CycleEngine] — this file only turns
 * day-of-cycle numbers into dates and one line of copy.
 *
 * **Every word of that copy says "predicted" or "estimated", and none of it says "confirmed".** The
 * engine assumes a fixed cycle length and a fixed 14-day luteal phase; it has no temperature, no LH
 * test and no observed ovulation behind it. It is arithmetic on the date she last entered. Wording
 * that implied otherwise would be a fertility claim this app cannot stand behind, so the card offers
 * a prediction and is explicit that it is one.
 */
object OvulationLogic {

    fun compute(
        settings: CycleSettings?,
        today: LocalDate = LocalDate.now(),
    ): OvulationInsights {
        if (settings == null) return OvulationInsights()

        val info = CycleEngine.getCyclePhase(settings, today)

        // The Monday-of-the-cycle, so day-of-cycle numbers become real dates. dayOfCycle is 1-based.
        val cycleStart = today.minusDays((info.dayOfCycle - 1).toLong())
        fun dateOfCycleDay(day: Int): LocalDate = cycleStart.plusDays((day - 1).toLong())

        // One classification per day of this cycle, straight from the engine that paints the Track
        // calendar — so the ribbon and the calendar can never disagree about which days are fertile.
        val dayTypes = (1..settings.cycleLength).map { day ->
            CycleEngine.dayTypeFor(CycleEngine.getCyclePhase(settings, dateOfCycleDay(day)))
        }

        return OvulationInsights(
            hasData = true,
            cycleLength = settings.cycleLength,
            currentDayOfCycle = info.dayOfCycle,
            currentPhase = info.phase,
            ovulationDay = info.ovulationDay,
            ovulationDate = dateOfCycleDay(info.ovulationDay),
            fertileWindowStart = dateOfCycleDay(info.fertileWindow.startDay),
            fertileWindowEnd = dateOfCycleDay(info.fertileWindow.endDay),
            dayTypes = dayTypes,
            insight = insightFor(info.dayOfCycle, info.ovulationDay, CycleEngine.dayTypeFor(info), info.phase),
        )
    }

    /**
     * Branches in [CycleEngine.dayTypeFor]'s precedence — period, then ovulation, then fertile, then
     * the rest — rather than a fresh order of my own. A short cycle with a long period can put day 5
     * inside both the period and the fertile window, and when the calendar paints that day as a
     * period day this card must not call it a fertile one.
     */
    private fun insightFor(dayOfCycle: Int, ovulationDay: Int, dayType: DayType, phase: Phase): String =
        when {
            dayType == DayType.PERIOD -> {
                val until = ovulationDay - dayOfCycle
                "Day $dayOfCycle of your cycle, on your period. Ovulation is estimated in $until " +
                    "${dayLabel(until)}."
            }
            dayType == DayType.OVULATION ->
                "Day $dayOfCycle — today is your predicted ovulation day. It's an estimate from your " +
                    "cycle length, not a confirmed reading."
            dayType == DayType.FERTILE ->
                "Day $dayOfCycle — you're in your predicted fertile window."
            phase == Phase.LUTEAL ->
                "Day $dayOfCycle — your luteal phase. Ovulation is estimated to have passed."
            else -> {
                val until = ovulationDay - dayOfCycle
                "Day $dayOfCycle — $until ${dayLabel(until)} until your predicted ovulation."
            }
        }

    private fun dayLabel(days: Int) = if (days == 1) "day" else "days"
}
