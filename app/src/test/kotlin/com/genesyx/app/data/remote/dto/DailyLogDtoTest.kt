package com.genesyx.app.data.remote.dto

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Wire-format guard for the intimacy field. The release plan lets the client ship BEFORE the
 * server column exists only because a null `sexual_activity` is omitted from the payload —
 * PostgREST rejects an upsert naming an unknown column, which would break every daily-log push.
 * If someone removes the `= null` default (or turns encodeDefaults on), this test is the tripwire.
 */
class DailyLogDtoTest {

    // Mirrors supabase-kt's serializer config: encodeDefaults off, unknown keys ignored.
    private val json = Json { ignoreUnknownKeys = true }

    private fun dto(sexualActivity: Boolean? = null) = DailyLogDto(
        userId = "user-a",
        date = "2026-08-10",
        waterMl = 500,
        sexualActivity = sexualActivity,
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
}
