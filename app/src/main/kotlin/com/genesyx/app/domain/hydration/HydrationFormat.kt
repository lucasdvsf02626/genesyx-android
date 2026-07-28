package com.genesyx.app.domain.hydration

import java.util.Locale

import kotlin.math.roundToInt

/**
 * The one rule for showing a water amount, in the customer's chosen [HydrationUnit]:
 * - ML: below a litre in millilitres ("600ml"), from a litre up in litres to one decimal ("1.6 L").
 *   This replaces the mix of "%.1f L", "%.1fL" and "${ml}ml" that put "0.0 / 2.4 L goal" beside
 *   "2400ml to go" on the same card.
 * - CUPS: metric 250ml cups to one decimal, whole numbers plain ("2 cups", "2.5 cups", "1 cup").
 *
 * Amounts and totals only — per-day deltas ("+700ml/day") stay in millilitres, because a
 * difference is a rate, not a pour.
 */
object HydrationFormat {

    /** Metric cup. Client-confirmed 28 Jul 2026; iOS must use the same constant. */
    const val CUP_ML = 250

    fun format(ml: Int, unit: HydrationUnit = HydrationUnit.ML): String = when (unit) {
        HydrationUnit.ML -> if (ml < 1000) "${ml}ml" else String.format(Locale.UK, "%.1f L", ml / 1000f)
        HydrationUnit.CUPS -> cups(ml)
    }

    private fun cups(ml: Int): String {
        val tenths = (ml * 10.0 / CUP_ML).roundToInt()
        val label = if (tenths % 10 == 0) "${tenths / 10}" else String.format(Locale.UK, "%.1f", tenths / 10.0)
        return if (label == "1") "1 cup" else "$label cups"
    }
}
