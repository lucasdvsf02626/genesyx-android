package com.genesyx.app.data.remote.dto

import com.genesyx.app.data.local.entity.ConsentEventEntity
import com.genesyx.app.domain.consent.ConsentPolicy
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.time.OffsetDateTime

/**
 * Wire model for the Supabase `consent_events` row. THE SCHEMA IS OWNED BY THE iOS REPO
 * (`supabase/migrations/20260818_consent_events.sql`): columns are `occurred_at` (not the
 * Room entity's `recordedAt` name) and a NOT NULL `version` — the consent-copy version she was
 * shown, [com.genesyx.app.domain.consent.ConsentPolicy.WIRE_VERSION] for events this app
 * records. Code 22 shipped assuming `recorded_at`/no version and every push 400d
 * ("Could not find the 'recorded_at' column") — see CHANGELOG 1 Sep 2026 (smoke test).
 * Append-only on both sides: rows are only ever inserted, never updated.
 */
@Serializable
data class ConsentEventDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    val version: String,
    val action: String,
    @SerialName("occurred_at") val occurredAt: String,
)

/** Postgres returns timestamptz with an offset; the app models timestamps as local wall-clock. */
private fun parseTs(s: String): LocalDateTime =
    runCatching { OffsetDateTime.parse(s).toLocalDateTime() }.getOrElse { LocalDateTime.parse(s) }

fun ConsentEventEntity.toDto(): ConsentEventDto = ConsentEventDto(
    id = id,
    userId = userId,
    // The entity deliberately has no version column (a Room migration for a value that is
    // constant per app build buys nothing): only locally-recorded events are ever pushed, and
    // they were all shown this build's copy.
    version = ConsentPolicy.WIRE_VERSION,
    action = action,
    occurredAt = recordedAt.toString(),
)

fun ConsentEventDto.toEntity(): ConsentEventEntity = ConsentEventEntity(
    id = id,
    userId = userId,
    action = action,
    recordedAt = parseTs(occurredAt),
)
