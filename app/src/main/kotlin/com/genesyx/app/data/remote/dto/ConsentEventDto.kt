package com.genesyx.app.data.remote.dto

import com.genesyx.app.data.local.entity.ConsentEventEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.time.OffsetDateTime

/** Wire model for the Supabase `consent_events` row (snake_case; timestamps ISO). Append-only on
 *  both sides: rows are only ever inserted, so upsert-by-id can never rewrite the trail. */
@Serializable
data class ConsentEventDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    val action: String,
    @SerialName("recorded_at") val recordedAt: String,
)

/** Postgres returns timestamptz with an offset; the app models timestamps as local wall-clock. */
private fun parseTs(s: String): LocalDateTime =
    runCatching { OffsetDateTime.parse(s).toLocalDateTime() }.getOrElse { LocalDateTime.parse(s) }

fun ConsentEventEntity.toDto(): ConsentEventDto = ConsentEventDto(
    id = id,
    userId = userId,
    action = action,
    recordedAt = recordedAt.toString(),
)

fun ConsentEventDto.toEntity(): ConsentEventEntity = ConsentEventEntity(
    id = id,
    userId = userId,
    action = action,
    recordedAt = parseTs(recordedAt),
)
