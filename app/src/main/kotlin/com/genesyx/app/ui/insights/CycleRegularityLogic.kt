package com.genesyx.app.ui.insights

import com.genesyx.app.domain.cycle.CycleEngine
import com.genesyx.app.domain.model.CycleSettings

/**
 * Pure cycle-regularity computation.
 *
 * **This card is not a trend, and must never be drawn as one.** The app stores her *current* cycle
 * setup — one `cycleLength` in `cycle_settings` — and nothing at all about the length of the cycles
 * before it. There is no history here to chart. A line or a bar chart would be inventing variation
 * out of a single stored number, so the card states that number, sets it beside the typical range,
 * and says plainly that it is her setup rather than a measurement.
 *
 * The typical range is [CycleEngine.CYCLE_LENGTH_RANGE] (21–35 days), the same bounds the cycle
 * settings dialog already enforces — quoted from there rather than restated, so the card and the
 * editor cannot drift apart.
 */
object CycleRegularityLogic {

    val typicalRange = CycleEngine.CYCLE_LENGTH_RANGE
    val typicalMin: Int get() = typicalRange.first
    val typicalMax: Int get() = typicalRange.last

    fun compute(settings: CycleSettings?): CycleRegularityInsights {
        val length = settings?.cycleLength ?: return CycleRegularityInsights()

        val inTypicalRange = length in typicalRange
        return CycleRegularityInsights(
            hasData = true,
            cycleLength = length,
            inTypicalRange = inTypicalRange,
            insight = insightFor(length, inTypicalRange),
        )
    }

    /**
     * Outside the range this says "worth a word with your GP" and nothing more. It does not name a
     * condition, and it does not call her cycle irregular — a cycle length she set once is not a
     * diagnosis, and this app has no evidence to offer one.
     */
    private fun insightFor(length: Int, inTypicalRange: Boolean): String {
        val setup = "Your cycle is set to $length days."
        val reading = if (inTypicalRange) {
            "That sits inside the typical $typicalMin–$typicalMax day range."
        } else {
            "That sits outside the typical $typicalMin–$typicalMax day range, which is worth a word " +
                "with your GP if it's new for you."
        }
        // The honesty guard: no cycle-to-cycle history exists, so no variation can be claimed.
        return "$setup $reading This is the setup you saved, not a measurement of past cycles — " +
            "Genesyx doesn't store cycle-by-cycle history yet."
    }
}
