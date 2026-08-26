package com.genesyx.app.domain.model

import com.genesyx.app.data.remote.dto.DailyLogDto
import com.genesyx.app.domain.content.FoodGroup
import com.genesyx.app.domain.content.recipeContent
import com.genesyx.app.domain.ph.PhStatus
import com.genesyx.app.domain.streaks.StreakEngine
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * DATA contract: what a logged day means, what syncs, what must never qualify a streak.
 */
class DataContractTest {

    @Test
    fun `intimacy alone is not a meaningful day`() {
        assertFalse(DailyLog(sexualActivity = true).isMeaningful())
        assertFalse(DailyLog(sexualActivity = false).isMeaningful())
        assertFalse(DailyLog().isMeaningful())
    }

    @Test
    fun `food groups mood water sleep supplements notes and energy each qualify`() {
        assertTrue(DailyLog(foodGroups = setOf("vegetables")).isMeaningful())
        assertTrue(DailyLog(mood = Mood.OKAY).isMeaningful())
        assertTrue(DailyLog(waterMl = 250).isMeaningful())
        assertTrue(DailyLog(sleepMinutes = 0).isMeaningful())
        assertTrue(DailyLog(supplements = setOf("Folate")).isMeaningful())
        assertTrue(DailyLog(notes = "travel").isMeaningful())
        assertTrue(DailyLog(energy = EnergyLevel.LOW).isMeaningful())
        assertTrue(DailyLog(symptoms = setOf("cramp")).isMeaningful())
    }

    @Test
    fun `food group raw tokens are the six shared with iOS`() {
        assertEquals(
            setOf("vegetables", "fruit", "starchyCarbs", "protein", "dairy", "oilsAndFats"),
            FoodGroup.entries.map { it.raw }.toSet(),
        )
        assertEquals(1, FoodGroup.knownCount(setOf("vegetables", "not-a-group")))
        assertEquals(0, FoodGroup.knownCount(emptySet()))
    }

    @Test
    fun `every recipe group token is a known food group`() {
        recipeContent.forEach { recipe ->
            recipe.groups.forEach { raw ->
                assertTrue("${recipe.id} unknown group $raw", FoodGroup.fromRaw(raw) != null)
            }
        }
    }

    @Test
    fun `zero sleep is recorded data not an empty day`() {
        assertTrue(DailyLog(sleepMinutes = 0).isMeaningful())
        assertFalse(DailyLog(sleepMinutes = null).isMeaningful())
    }

    @Test
    fun `blank notes do not qualify a day`() {
        assertFalse(DailyLog(notes = "").isMeaningful())
        assertFalse(DailyLog(notes = "   ").isMeaningful())
        assertTrue(DailyLog(notes = "travel").isMeaningful())
    }

    @Test
    fun `zero water is not a logged day but any sip is`() {
        assertFalse(DailyLog(waterMl = 0).isMeaningful())
        assertTrue(DailyLog(waterMl = 1).isMeaningful())
    }

    @Test
    fun `hydration storage unit is millilitres and the goal cannot be zero`() {
        assertTrue(StreakEngine.DEFAULT_GOAL_ML in StreakEngine.GOAL_RANGE_ML)
        assertFalse(0 in StreakEngine.GOAL_RANGE_ML)
        assertEquals(200, StreakEngine.GOAL_STEP_ML)
    }

    @Test
    fun `pH classify matches the two-band contract`() {
        assertEquals(PhStatus.HEALTHY, PhStatus.classify(PhStatus.HEALTHY_MAX))
        assertEquals(PhStatus.ELEVATED, PhStatus.classify(PhStatus.HEALTHY_MAX + PhStatus.STEP))
        assertTrue(PhStatus.DEFAULT in PhStatus.HEALTHY_MIN..PhStatus.HEALTHY_MAX)
        assertTrue(PhStatus.MIN <= PhStatus.HEALTHY_MIN)
        assertTrue(PhStatus.MAX > PhStatus.HEALTHY_MAX)
    }

    /**
     * Two different rules share this payload, and the difference is whether the user can *clear*
     * the field.
     *
     * `sexual_activity` stays off the wire while unset: the column is `NOT NULL DEFAULT false`, so
     * an explicit null would fail the whole upsert. It is the only field that still does.
     *
     * A clearable field is the opposite case: PostgREST builds `columns=` from the body, so omitting
     * it means the column is never written and the stale server value returns on the next pull.
     * `water_ml` used to be asserted absent at zero here — that was the silent-clear bug written
     * down as a requirement, and removing her last glass never synced because of it. `food_groups`
     * was asserted absent for a further release on the belief that the column might not exist; it
     * has been live since 13 Aug 2026. Both now carry `@EncodeDefault`. See `DailyLogDtoTest`.
     */
    @Test
    fun `daily log wire names are snake_case, omitting only the column that must not be nulled`() {
        val json = Json { ignoreUnknownKeys = true }
        val empty = DailyLogDto(userId = "u", date = "2026-08-17")
        val encoded = json.encodeToString(empty)
        // A day with no groups ticked is "she cleared them", and must reach the column.
        assertTrue(encoded.contains("\"food_groups\":[]"))
        assertFalse(encoded.contains("sexual_activity"))
        // A zero pour is a real value — "she removed her last glass" — and must reach the column.
        assertTrue(encoded.contains("\"water_ml\":0"))
        assertTrue(encoded.contains("user_id"))
        val withWater = json.encodeToString(DailyLogDto(userId = "u", date = "2026-08-17", waterMl = 250))
        assertTrue(withWater.contains("\"water_ml\":250"))
        val withGroups = json.encodeToString(
            DailyLogDto(userId = "u", date = "2026-08-17", foodGroups = listOf("vegetables")),
        )
        assertTrue(withGroups.contains("\"food_groups\":[\"vegetables\"]"))
    }

    @Test
    fun `unfamiliar food-group tokens survive a round trip`() {
        val json = Json { ignoreUnknownKeys = true }
        val decoded = json.decodeFromString<DailyLogDto>(
            """{"user_id":"u","date":"2026-08-17","food_groups":["fermented","vegetables"]}""",
        )
        assertEquals(listOf("fermented", "vegetables"), decoded.foodGroups)
        assertEquals(1, FoodGroup.knownCount(decoded.foodGroups.toSet()))
    }
}
