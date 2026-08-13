package com.genesyx.app.data.remote.dto

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Wire-format guard for the fields that are optional on the server. The release plan lets the client
 * ship BEFORE a column exists only because an unset value is omitted from the payload — PostgREST
 * rejects an upsert naming an unknown column, which would break every daily-log push. If someone
 * removes a default (or turns encodeDefaults on), this test is the tripwire.
 */
class DailyLogDtoTest {

    // Mirrors supabase-kt's serializer config: encodeDefaults off, unknown keys ignored.
    private val json = Json { ignoreUnknownKeys = true }

    private fun dto(
        sexualActivity: Boolean? = null,
        foodGroups: List<String> = emptyList(),
    ) = DailyLogDto(
        userId = "user-a",
        date = "2026-08-10",
        waterMl = 500,
        sexualActivity = sexualActivity,
        foodGroups = foodGroups,
    )

    @Test
    fun `an unrecorded intimacy field stays off the wire`() {
        assertFalse(json.encodeToString(dto()).contains("sexual_activity"))
    }

    @Test
    fun `a recorded intimacy field is sent`() {
        assertTrue(json.encodeToString(dto(sexualActivity = true)).contains("\"sexual_activity\":true"))
    }

    @Test
    fun `a server row without the column decodes as not recorded`() {
        val decoded = json.decodeFromString<DailyLogDto>(
            """{"user_id":"user-a","date":"2026-08-10","water_ml":500}""",
        )
        assertTrue(decoded.sexualActivity == null)
    }

    @Test
    fun `a day with no meals recorded keeps food groups off the wire`() {
        assertFalse(json.encodeToString(dto()).contains("food_groups"))
    }

    /** Android cannot log meals yet, but it must not blank the ones iOS wrote when it pushes a row. */
    @Test
    fun `food groups carried from the server are sent back`() {
        val encoded = json.encodeToString(dto(foodGroups = listOf("vegetables", "protein")))
        assertTrue(encoded.contains("\"food_groups\":[\"vegetables\",\"protein\"]"))
    }

    @Test
    fun `a server row without food groups decodes as no meals recorded`() {
        val decoded = json.decodeFromString<DailyLogDto>(
            """{"user_id":"user-a","date":"2026-08-10","water_ml":500}""",
        )
        assertTrue(decoded.foodGroups.isEmpty())
    }

    /** Tokens, not an enum: a group iOS adds later must decode here rather than throw. */
    @Test
    fun `an unfamiliar food group decodes intact`() {
        val decoded = json.decodeFromString<DailyLogDto>(
            """{"user_id":"user-a","date":"2026-08-10","food_groups":["fermented"]}""",
        )
        assertTrue(decoded.foodGroups == listOf("fermented"))
    }
}
