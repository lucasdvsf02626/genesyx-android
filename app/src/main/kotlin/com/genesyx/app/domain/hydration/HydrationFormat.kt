package com.genesyx.app.domain.hydration

import java.util.Locale

/**
 * The one rule for showing a water amount: below a litre it reads in millilitres ("600ml"), from a
 * litre up in litres to one decimal ("1.6 L"). This replaces the mix of "%.1f L", "%.1fL" and
 * "${ml}ml" that put "0.0 / 2.4 L goal" beside "2400ml to go" on the same card.
 *
 * Amounts and totals only — per-day deltas ("+700ml/day") stay in millilitres, because a
 * difference is a rate, not a pour.
 */
object HydrationFormat {
    fun format(ml: Int): String =
        if (ml < 1000) "${ml}ml" else String.format(Locale.UK, "%.1f L", ml / 1000f)
}
