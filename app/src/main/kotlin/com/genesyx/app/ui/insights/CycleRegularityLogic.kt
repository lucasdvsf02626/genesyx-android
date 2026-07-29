package com.genesyx.app.ui.insights

import com.genesyx.app.domain.cycle.CycleEngine
import com.genesyx.app.domain.model.CycleSettings
import com.genesyx.app.domain.model.Phase
import java.time.LocalDate

/**
 * One contiguous run of days sharing a phase, 1-based and inclusive at both ends.
 */
data class PhaseSegment(val phase: Phase, val startDay: Int, val endDay: Int) {
    val dayCount: Int get() = endDay - startDay + 1
    operator fun contains(dayOfCycle: Int): Boolean = dayOfCycle in startDay..endDay
}

/**
 * Pure cycle-phase-timeline computation.
 *
 * **This card is not a trend, and must never be drawn as one.** The app stores her *current* cycle
 * setup — one `cycleLength` in `cycle_settings` — and nothing at all about the length of the cycles
 * before it. There is no history here to chart. A phase timeline of the *current* cycle is position,
 * not history — it shows where today falls in the one setup she saved, and still claims nothing
 * about cycle-to-cycle variation.
 *
 * The typical range is [CycleEngine.CYCLE_LENGTH_RANGE] (21–35 days), the same bounds the cycle
 * settings dialog already enforces — quoted from there rather than restated, so the card and the
 * editor cannot drift apart.
 */
object CycleRegularityLogic {

    val typicalRange = CycleEngine.CYCLE_LENGTH_RANGE
    val typicalMin: Int get() = typicalRange.first
    val typicalMax: Int get() = typicalRange.last

    /**
     * A segment never renders narrower than this many days' worth of bar. The ovulatory phase is a
     * single engine day (~3% of a 35-day cycle), which would vanish at card width. The marker uses
     * the same widened weights ([markerFraction]), so it always sits inside the segment it names.
     */
    const val MIN_VISUAL_DAYS = 2f

    const val DISCLAIMER = "Phase dates are estimates based on your logged cycle information."

    /** Legend labels. The app says "Period" everywhere — never "Menstrual" — and the "Phase" suffix
     *  in [com.genesyx.app.domain.content.CycleContent.phaseLabel] would double up against the
     *  card's own "Current phase:" prefix. */
    val shortPhaseLabel: Map<Phase, String> = mapOf(
        Phase.PERIOD to "Period",
        Phase.FOLLICULAR to "Follicular",
        Phase.OVULATORY to "Ovulatory",
        Phase.LUTEAL to "Luteal",
    )

    /** One neutral wellbeing line per phase — no diagnosis, no fertility or conception advice. */
    val phaseInsight: Map<Phase, String> = mapOf(
        Phase.PERIOD to
            "Energy often runs lower during your period — extra rest and warm meals can help.",
        Phase.FOLLICULAR to
            "Energy tends to build through the follicular phase — a natural window for getting things moving.",
        Phase.OVULATORY to
            "Around the ovulatory phase, many people notice their energy at its peak.",
        Phase.LUTEAL to
            "Energy can wind down in the luteal phase — gentler routines and steady sleep help.",
    )

    fun compute(
        settings: CycleSettings?,
        today: LocalDate = LocalDate.now(),
    ): CycleRegularityInsights {
        val length = settings?.cycleLength ?: return CycleRegularityInsights()

        val inTypicalRange = length in typicalRange
        val info = CycleEngine.getCyclePhase(settings, today)
        val segs = segments(settings)
        val currentIndex = segs.indexOfFirst { info.dayOfCycle in it }
        val next = segs.getOrNull(currentIndex + 1)

        return CycleRegularityInsights(
            hasData = true,
            cycleLength = length,
            inTypicalRange = inTypicalRange,
            insight = insightFor(length, inTypicalRange),
            segments = segs,
            dayOfCycle = info.dayOfCycle,
            currentPhase = info.phase,
            nextPhase = next?.phase ?: segs.first().phase,
            // Wrapping from the last segment counts calendar days to the next cycle's day 1 —
            // always ≥ 1, because "in 0 days" is not a sentence this card should ever show.
            daysUntilNextPhase = next?.let { it.startDay - info.dayOfCycle }
                ?: (length - info.dayOfCycle + 1),
            markerFraction = markerFraction(segs, info.dayOfCycle),
            phaseInsightLine = phaseInsight.getValue(info.phase),
        )
    }

    /**
     * Run-length-encodes [CycleEngine.getCyclePhase]'s answer for every day of the cycle, rather
     * than restating its when-ladder — so the bar cannot disagree with the phase label the rest of
     * the app shows. This also means the list is whatever the engine says it is: a long period in a
     * short cycle swallows the ovulation day, and the result is two segments, not four. The UI
     * renders the list it gets and never assumes four.
     */
    fun segments(settings: CycleSettings): List<PhaseSegment> {
        fun dateOfCycleDay(day: Int): LocalDate = settings.lastPeriodDate.plusDays((day - 1).toLong())

        val out = ArrayList<PhaseSegment>()
        for (day in 1..settings.cycleLength) {
            val phase = CycleEngine.getCyclePhase(settings, dateOfCycleDay(day)).phase
            val last = out.lastOrNull()
            if (last != null && last.phase == phase) {
                out[out.size - 1] = last.copy(endDay = day)
            } else {
                out.add(PhaseSegment(phase, day, day))
            }
        }
        return out
    }

    /** The Row weight for each segment — day count, floored at [MIN_VISUAL_DAYS]. */
    fun visualWeights(segments: List<PhaseSegment>): List<Float> =
        segments.map { maxOf(it.dayCount.toFloat(), MIN_VISUAL_DAYS) }

    /**
     * Where along the bar today's marker sits, 0..1 — piecewise-linear through the same widened
     * weights the bar is drawn with, placing the marker at the middle of today's day. Sharing the
     * bar's coordinate system is the point: with a true-days fraction the widened ovulatory segment
     * would push the marker into the wrong segment near its boundary.
     */
    fun markerFraction(segments: List<PhaseSegment>, dayOfCycle: Int): Float {
        val weights = visualWeights(segments)
        val total = weights.sum()
        var before = 0f
        segments.forEachIndexed { i, seg ->
            if (dayOfCycle in seg) {
                val within = (dayOfCycle - seg.startDay + 0.5f) / seg.dayCount
                return ((before + within * weights[i]) / total).coerceIn(0f, 1f)
            }
            before += weights[i]
        }
        return 0f
    }

    /**
     * Outside the range this says "worth a word with your GP" and nothing more. It does not name a
     * condition, and it does not call her cycle irregular — a cycle length she set once is not a
     * diagnosis, and this app has no evidence to offer one. Either way the honesty guard stays: no
     * cycle-to-cycle history exists, so no variation can be claimed.
     */
    private fun insightFor(length: Int, inTypicalRange: Boolean): String =
        if (inTypicalRange) {
            "This timeline comes from the cycle length you saved — not a measurement of past cycles."
        } else {
            "Your cycle is set to $length days, outside the typical $typicalMin–$typicalMax day " +
                "range — worth a word with your GP if it's new for you. This is the setup you " +
                "saved, not a measurement of past cycles."
        }
}
