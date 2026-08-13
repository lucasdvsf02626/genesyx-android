package com.genesyx.app.domain.model

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

/** Which sitting a logged meal belongs to. Wire values are stored in Room; never rename one after
 *  it ships or existing rows stop mapping. */
enum class MealType(val wire: String, val label: String) {
    BREAKFAST("breakfast", "Breakfast"),
    LUNCH("lunch", "Lunch"),
    DINNER("dinner", "Dinner"),
    SNACK("snack", "Snack"),
    ;

    companion object {
        fun fromWire(value: String?): MealType = entries.firstOrNull { it.wire == value } ?: SNACK
    }
}

/** A nutrient the user can tag a meal with — a small, curated set relevant to fertility prep, not a
 *  full nutrition database. Wire values are stored; labels are shown. */
enum class Nutrient(val wire: String, val label: String) {
    PROTEIN("protein", "Protein"),
    IRON("iron", "Iron"),
    FOLATE("folate", "Folate"),
    OMEGA_3("omega_3", "Omega-3"),
    FIBRE("fibre", "Fibre"),
    CALCIUM("calcium", "Calcium"),
    VITAMIN_C("vitamin_c", "Vitamin C"),
    ;

    companion object {
        fun fromWire(value: String?): Nutrient? = entries.firstOrNull { it.wire == value }
    }
}

/**
 * One meal the user logged for a given day. Local-only (Room is the only home — no Supabase mirror,
 * no cross-platform contract), so unlike [UserSupplement] this has no sync bookkeeping.
 */
data class MealEntry(
    val id: String = UUID.randomUUID().toString(),
    val date: LocalDate,
    val type: MealType,
    val description: String,
    val nutrients: List<Nutrient> = emptyList(),
    val loggedAt: LocalDateTime = LocalDateTime.now(),
) {
    companion object {
        /** A description is trimmed client-side and capped so the row and card stay sane. */
        const val DESCRIPTION_MAX_LENGTH = 120
    }
}
