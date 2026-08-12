package com.genesyx.app.data.remote.dto

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pins the cross-platform wire shape: snake_case `user_id`, and `answers` as a plain JSON object. */
class QuizAnswersDtoTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `serializes answers as a jsonb object keyed by question id`() {
        val dto = QuizAnswersDto("u1", mapOf("stage" to "trying", "gender" to "girl"))
        val encoded = json.encodeToString(QuizAnswersDto.serializer(), dto)
        assertTrue(encoded.contains("\"user_id\":\"u1\""))
        assertTrue(encoded.contains("\"answers\":{"))
        assertTrue(encoded.contains("\"gender\":\"girl\""))
    }

    @Test
    fun `decodes a server row, ignoring server-only columns like updated_at`() {
        val row = """{"user_id":"u1","answers":{"cycle":"very"},"updated_at":"2026-08-12T00:00:00Z"}"""
        val dto = json.decodeFromString(QuizAnswersDto.serializer(), row)
        assertEquals("u1", dto.userId)
        assertEquals(mapOf("cycle" to "very"), dto.answers)
    }

    @Test
    fun `a missing answers object decodes to empty`() {
        val dto = json.decodeFromString(QuizAnswersDto.serializer(), """{"user_id":"u1"}""")
        assertEquals(emptyMap<String, String>(), dto.answers)
    }
}
