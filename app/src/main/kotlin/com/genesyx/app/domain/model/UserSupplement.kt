package com.genesyx.app.domain.model

import java.util.UUID

/** When the user takes a supplement. Wire values match the `user_supplements.time_of_day` CHECK
 *  constraint — a cross-platform contract, so never rename one after a client ships. */
enum class SupplementTime(val wire: String, val label: String) {
    MORNING("morning", "Morning"),
    AFTERNOON("afternoon", "Afternoon"),
    EVENING("evening", "Evening"),
    ANYTIME("anytime", "Anytime"),
    ;

    companion object {
        fun fromWire(value: String?): SupplementTime? = entries.firstOrNull { it.wire == value }
    }
}

/**
 * A supplement the user added themselves ("Add your own supplement"). Mirrors Supabase
 * `user_supplements` (docs/migrations/2026-07-29_user_supplements.sql). Distinct from [Supplement],
 * the fixed suggested plan: these are the user's own entries, with free-text name and dose.
 */
data class UserSupplement(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val dose: String? = null,
    val timeOfDay: SupplementTime? = null,
    /** Set when the entry was added from the Genesyx catalogue; null for the user's own. */
    val productId: String? = null,
) {
    companion object {
        /** Server contract: name is trimmed client-side, 1..60 chars. */
        const val NAME_MAX_LENGTH = 60
    }
}
