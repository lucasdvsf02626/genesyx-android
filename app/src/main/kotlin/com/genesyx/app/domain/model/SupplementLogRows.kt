package com.genesyx.app.domain.model

/** One row in the Log's Supplements dialog: what the user sees, and the string toggled in storage. */
data class SupplementRowSpec(val display: String, val stored: String)

/**
 * The rows the Supplements dialog shows for a given day, derived purely so the "nothing logged is
 * ever invisible" rule can be tested without Compose.
 *
 * Three groups, in order:
 * 1. **Built-ins** — the whole loggable vocabulary, toggling their wire names.
 * 2. **Custom** — the user's own current entries (Nutrition → Your supplements), by name.
 * 3. **Orphans** — any string already stored on this day that is neither a built-in wire name nor a
 *    current custom entry: a supplement logged under a name that has since been renamed, or written
 *    by another device/build. Without a row it would still sit in the stored set (nothing removes
 *    it) but be un-untoggleable and invisible — so identity is not left to rely on the current,
 *    mutable custom-entry name. Orphans are shown by their stored string and sorted for determinism.
 *
 * Storage is unchanged: every row toggles the exact string it reads, so the cross-platform
 * `daily_logs.supplements` contract is untouched. Robust ID-based adherence is a separate,
 * cross-platform decision (see APP_INVENTORY §Supplements).
 */
object SupplementLogRows {

    fun forDay(selected: Set<String>, custom: List<String>): List<SupplementRowSpec> {
        val builtIn = Supplement.loggable.map { SupplementRowSpec(it.displayName, it.wireName) }
        val builtInWire = Supplement.loggable.map { it.wireName }.toSet()
        val customRows = custom.map { SupplementRowSpec(it, it) }
        val customSet = custom.toSet()

        val orphans = selected
            .filter { it !in builtInWire && it !in customSet }
            .sorted()
            .map { SupplementRowSpec(it, it) }

        return builtIn + customRows + orphans
    }
}
