package com.genesyx.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.genesyx.app.domain.model.MealEntry
import com.genesyx.app.domain.model.MealType
import com.genesyx.app.domain.model.Nutrient
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Room mirror of a logged meal. Local-only (no Supabase table), so no sync columns. Scoped by
 * [userId] like every other table so accounts stay isolated on a shared device; index (userId, date)
 * matches the "today's meals" read. [nutrients] is a wire-value list, stored via the string-list
 * converter.
 */
@Entity(tableName = "meal_entries", indices = [Index(value = ["userId", "date"])])
data class MealEntryEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val date: LocalDate,
    val mealType: String,
    val description: String,
    val nutrients: List<String>,
    val loggedAt: LocalDateTime,
)

fun MealEntryEntity.toDomain(): MealEntry = MealEntry(
    id = id,
    date = date,
    type = MealType.fromWire(mealType),
    description = description,
    nutrients = nutrients.mapNotNull { Nutrient.fromWire(it) },
    loggedAt = loggedAt,
)

fun MealEntry.toEntity(userId: String): MealEntryEntity = MealEntryEntity(
    id = id,
    userId = userId,
    date = date,
    mealType = type.wire,
    description = description,
    nutrients = nutrients.map { it.wire },
    loggedAt = loggedAt,
)
