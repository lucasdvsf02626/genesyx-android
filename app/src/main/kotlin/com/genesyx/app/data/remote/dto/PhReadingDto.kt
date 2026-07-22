package com.genesyx.app.data.remote.dto

import com.genesyx.app.data.local.entity.PhReadingEntity
import com.genesyx.app.data.local.entity.PhSyncStatus
import com.genesyx.app.domain.model.PhMeasurement
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.time.OffsetDateTime

/** Wire model for the Supabase `ph_readings` row (snake_case; timestamps ISO). */
@Serializable
data class PhReadingDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("ph_value") val phValue: Double,
    @SerialName("recorded_at") val recordedAt: String,
    val notes: String? = null,
    // Nullable so rows written before the server-side migration (no column yet) decode as legacy urine.
    @SerialName("measurement_type") val measurementType: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("deleted_at") val deletedAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)

/** Postgres returns timestamptz with an offset; the app models timestamps as local wall-clock. */
private fun parseTs(s: String): LocalDateTime =
    runCatching { OffsetDateTime.parse(s).toLocalDateTime() }.getOrElse { LocalDateTime.parse(s) }

fun PhReadingEntity.toDto(): PhReadingDto = PhReadingDto(
    id = id,
    userId = userId,
    phValue = phValue,
    recordedAt = recordedAt.toString(),
    notes = notes,
    measurementType = measurementType,
    updatedAt = updatedAt?.toString(),
    deletedAt = deletedAt?.toString(),
)

/** Pulled server row → local entity, marked SYNCED (server is authoritative on pull).
 *  A missing measurement_type (rows predating the server migration) is treated as legacy urine. */
fun PhReadingDto.toEntity(userId: String): PhReadingEntity = PhReadingEntity(
    id = id,
    userId = userId,
    phValue = phValue,
    recordedAt = parseTs(recordedAt),
    notes = notes,
    measurementType = measurementType ?: PhMeasurement.URINE,
    syncStatus = PhSyncStatus.SYNCED,
    updatedAt = updatedAt?.let { parseTs(it) },
    deletedAt = deletedAt?.let { parseTs(it) },
)
