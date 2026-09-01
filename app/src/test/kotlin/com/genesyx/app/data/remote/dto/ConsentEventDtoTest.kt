package com.genesyx.app.data.remote.dto

import com.genesyx.app.data.local.entity.ConsentEventEntity
import com.genesyx.app.domain.consent.ConsentPolicy
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

/**
 * Wire-format guard for `consent_events`. The schema is owned by the iOS repo's
 * `20260818_consent_events.sql`: the timestamp column is `occurred_at` and `version` is
 * NOT NULL. Code 22 shipped sending `recorded_at` and no version, so every push was rejected
 * ("Could not find the 'recorded_at' column") — these tests are the tripwire.
 */
class ConsentEventDtoTest {

    // Mirrors supabase-kt's serializer config.
    private val json = Json { ignoreUnknownKeys = true }

    private val entity = ConsentEventEntity(
        id = "e1",
        userId = "user-a",
        action = "granted",
        recordedAt = LocalDateTime.of(2026, 9, 1, 20, 30, 5),
    )

    @Test
    fun `the wire body carries occurred_at and version, never recorded_at`() {
        val body = json.encodeToString(entity.toDto())
        val keys = json.encodeToJsonElement(ConsentEventDto.serializer(), entity.toDto()).jsonObject.keys

        assertEquals(setOf("id", "user_id", "version", "action", "occurred_at"), keys)
        assertFalse(body.contains("recorded_at"))
        assertTrue(body.contains("\"occurred_at\":\"2026-09-01T20:30:05\""))
    }

    @Test
    fun `the version sent is the shared consent-copy version, verbatim`() {
        // Must equal iOS ConsentPolicy.currentVersion — one disclosure, one version string.
        assertEquals("2026-08-18.v2", ConsentPolicy.WIRE_VERSION)
        assertEquals(ConsentPolicy.WIRE_VERSION, entity.toDto().version)
    }

    @Test
    fun `a server row decodes through occurred_at with its offset stripped to wall-clock`() {
        val row = json.decodeFromString<ConsentEventDto>(
            """{"id":"e2","user_id":"user-a","version":"2026-08-18.v2","action":"withdrawn",""" +
                """"occurred_at":"2026-08-01T09:30:00+00:00"}""",
        )

        val decoded = row.toEntity()
        assertEquals("withdrawn", decoded.action)
        assertEquals(LocalDateTime.of(2026, 8, 1, 9, 30), decoded.recordedAt)
    }

    @Test
    fun `a round trip through the wire preserves the event`() {
        val back = entity.toDto().toEntity()
        assertEquals(entity.id, back.id)
        assertEquals(entity.userId, back.userId)
        assertEquals(entity.action, back.action)
        assertEquals(entity.recordedAt, back.recordedAt)
    }
}
