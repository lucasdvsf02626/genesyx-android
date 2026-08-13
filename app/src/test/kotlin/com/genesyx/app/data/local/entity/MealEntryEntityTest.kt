package com.genesyx.app.data.local.entity

import com.genesyx.app.domain.model.MealEntry
import com.genesyx.app.domain.model.MealType
import com.genesyx.app.domain.model.Nutrient
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class MealEntryEntityTest {

    @Test
    fun `domain to entity to domain round-trips every field`() {
        val meal = MealEntry(
            id = "meal-1",
            date = LocalDate.of(2026, 8, 12),
            type = MealType.LUNCH,
            description = "Lentil salad",
            nutrients = listOf(Nutrient.PROTEIN, Nutrient.IRON, Nutrient.FOLATE),
            loggedAt = LocalDateTime.of(2026, 8, 12, 13, 30),
        )

        val back = meal.toEntity(userId = "user-a").toDomain()

        assertEquals(meal, back)
    }

    @Test
    fun `entity wires nutrients and meal type as their stable wire values`() {
        val entity = MealEntry(
            date = LocalDate.of(2026, 8, 12),
            type = MealType.SNACK,
            description = "Almonds",
            nutrients = listOf(Nutrient.OMEGA_3, Nutrient.CALCIUM),
        ).toEntity(userId = "user-a")

        assertEquals("snack", entity.mealType)
        assertEquals(listOf("omega_3", "calcium"), entity.nutrients)
        assertEquals("user-a", entity.userId)
    }

    @Test
    fun `an unknown meal-type wire falls back to snack, unknown nutrients are dropped`() {
        val entity = MealEntryEntity(
            id = "x",
            userId = "user-a",
            date = LocalDate.of(2026, 8, 12),
            mealType = "brunch", // not a known type
            description = "Eggs",
            nutrients = listOf("iron", "unobtanium"), // second is unknown
            loggedAt = LocalDateTime.of(2026, 8, 12, 11, 0),
        )

        val domain = entity.toDomain()

        assertEquals(MealType.SNACK, domain.type)
        assertEquals(listOf(Nutrient.IRON), domain.nutrients)
    }
}
