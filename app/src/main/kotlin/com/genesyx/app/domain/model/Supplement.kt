package com.genesyx.app.domain.model

/**
 * The canonical supplement vocabulary — one list, replacing the two that disagreed.
 *
 * [id] is internal. [wireName] is what is written to Room and synced to `daily_logs.supplements`, a
 * `text[]` column iOS reads and writes too — so those strings are a cross-platform contract and must
 * not be renamed from here. Anything that needs a stable key uses [id]; anything that touches
 * storage goes through [fromWire] and [wireName]. That boundary is the whole point of this type.
 *
 * Dosage is held apart from the name on purpose. Baking "400–800 mcg" into the string is exactly
 * what made the plan list and the log list impossible to match against each other.
 */
enum class Supplement(
    val id: String,
    val displayName: String,
    val wireName: String,
    val dosageNote: String?,
) {
    FOLATE("folate", "Folate", "Folic acid", "400–800 mcg"),
    OMEGA_3("omega3", "Omega-3", "Omega-3", "DHA/EPA"),
    VITAMIN_D("vitamin_d", "Vitamin D", "Vitamin D", "600–1000 IU"),
    ZINC("zinc", "Zinc", "Zinc", "8–11 mg"),
    IRON("iron", "Iron", "Iron", null),
    ;

    companion object {
        /**
         * The default adherence plan — the denominator the Insights card scores against.
         *
         * Iron is [loggable] but deliberately outside it: it is recorded, not scored. Taking it
         * cannot push a bar past 100, and not taking it is not a miss.
         */
        val defaultPlan: List<Supplement> = listOf(FOLATE, OMEGA_3, VITAMIN_D, ZINC)

        /** Everything she can tick in the Log. */
        val loggable: List<Supplement> = entries

        private val byWire: Map<String, Supplement> = entries.associateBy { it.wireName }

        /**
         * Maps a stored string back to the vocabulary.
         *
         * Unknown strings — an older build, or a value another client wrote — return null rather
         * than being coerced or dropped. Callers keep the raw string they read, so nothing she
         * logged is ever silently lost; it simply does not score.
         */
        fun fromWire(value: String): Supplement? = byWire[value]
    }
}
