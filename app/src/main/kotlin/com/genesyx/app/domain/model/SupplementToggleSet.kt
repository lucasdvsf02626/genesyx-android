package com.genesyx.app.domain.model

/**
 * One row of the tap-to-toggle set on the Nutrition tab's plan card.
 *
 * [stored] is the exact string written to `daily_logs.supplements` — a built-in's wire name, or
 * her own entry's name verbatim — so a tap here and a tick in the Log dialog write the same thing.
 */
data class SupplementPlanEntry(
    val display: String,
    val stored: String,
    val dose: String?,
    /** The chip letter — "F", "O", "D", "Z" for the plan; the first letter of her own entries. */
    val initial: String,
    /** Set for the four bundled essentials; null for a supplement she added herself. */
    val builtIn: Supplement?,
) {
    val isCustom: Boolean get() = builtIn == null
}

/**
 * The toggleable set and its counts — the single definition the Nutrition card, the Track
 * summary and the Insights "Nutrition consistency" card all score against, so "N of M" is the
 * same N and the same M everywhere.
 *
 * The set is the bundled plan (four essentials, a constant — `genesyx_products` is deliberately
 * empty) followed by her own `user_supplements`, which join the denominator: adding one makes
 * "N of 4" read "N of 5". Matching is trimmed and case-insensitive, like [Supplement.fromWire].
 */
object SupplementToggleSet {

    fun build(
        custom: List<UserSupplement>,
        plan: List<Supplement> = Supplement.defaultPlan,
    ): List<SupplementPlanEntry> {
        val planEntries = plan.map { s ->
            SupplementPlanEntry(
                display = s.displayName,
                stored = s.wireName,
                dose = s.dosageNote,
                initial = s.chipInitial,
                builtIn = s,
            )
        }
        // A custom entry that names a plan item (by wire or display name) is the same thing seen
        // twice; one chip, not two toggling different strings.
        val taken = plan.flatMap { listOf(norm(it.wireName), norm(it.displayName)) }.toMutableSet()
        val customEntries = custom.mapNotNull { entry ->
            val name = entry.name.trim()
            if (name.isEmpty() || !taken.add(norm(name))) return@mapNotNull null
            SupplementPlanEntry(
                display = name,
                stored = name,
                dose = entry.dose?.takeIf { it.isNotBlank() },
                initial = name.take(1).uppercase(),
                builtIn = null,
            )
        }
        return planEntries + customEntries
    }

    /**
     * Whether [name] already names something in the set — a plan essential (by either its wire or
     * its display name) or one of [custom]. [excludingId] lets an edit keep its own name.
     *
     * A colliding name is not merely untidy. [build] drops it, so the row would be written to Room,
     * pushed to the server, and then never rendered anywhere: an invisible entry she cannot see,
     * toggle or remove. Callers on a write path must reject on this rather than save it.
     *
     * The comparison is [norm] — trimmed, case-insensitive — the same rule [build], [isLogged] and
     * `Supplement.fromWire` use, so "the same supplement" means one thing across the app.
     */
    fun namesSomethingIn(
        name: String,
        custom: List<UserSupplement>,
        excludingId: String? = null,
        plan: List<Supplement> = Supplement.defaultPlan,
    ): Boolean {
        val candidate = norm(name)
        if (candidate.isEmpty()) return false
        val planNames = plan.flatMap { listOf(norm(it.wireName), norm(it.displayName)) }
        if (candidate in planNames) return true
        return custom.any { it.id != excludingId && norm(it.name) == candidate }
    }

    /** Whether [entry] appears in a day's stored supplement names. */
    fun isLogged(entry: SupplementPlanEntry, logged: Collection<String>): Boolean =
        logged.any { norm(it) == norm(entry.stored) }

    /** How many of [entries] appear in a day's stored supplement names. */
    fun takenCount(entries: List<SupplementPlanEntry>, logged: Collection<String>): Int =
        entries.count { isLogged(it, logged) }

    /** The plan card's status line, iOS's strings: "None logged yet today" / "N of M logged today". */
    fun statusLine(taken: Int, size: Int): String =
        if (taken == 0) "None logged yet today" else "$taken of $size logged today"

    private fun norm(value: String): String = value.trim().lowercase()
}
