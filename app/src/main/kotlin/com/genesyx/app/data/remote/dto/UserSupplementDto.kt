package com.genesyx.app.data.remote.dto

import com.genesyx.app.data.local.entity.SupplementSyncStatus
import com.genesyx.app.data.local.entity.UserSupplementEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.time.OffsetDateTime

/** Wire model for the Supabase `user_supplements` row (snake_case; timestamps ISO). */
@Serializable
data class UserSupplementDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    val name: String,
    val dose: String? = null,
    @SerialName("time_of_day") val timeOfDay: String? = null,
    @SerialName("product_id") val productId: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("deleted_at") val deletedAt: String? = null,
)

/** Postgres returns timestamptz with an offset; the app models timestamps as local wall-clock. */
private fun parseTs(s: String): LocalDateTime =
    runCatching { OffsetDateTime.parse(s).toLocalDateTime() }.getOrElse { LocalDateTime.parse(s) }

fun UserSupplementEntity.toDto(): UserSupplementDto = UserSupplementDto(
    id = id,
    userId = userId,
    name = name,
    dose = dose,
    timeOfDay = timeOfDay,
    productId = productId,
    createdAt = createdAt.toString(),
    updatedAt = updatedAt?.toString(),
    deletedAt = deletedAt?.toString(),
)

/** Pulled server row → local entity, marked SYNCED (server is authoritative on pull). */
fun UserSupplementDto.toEntity(userId: String): UserSupplementEntity = UserSupplementEntity(
    id = id,
    userId = userId,
    name = name,
    dose = dose,
    timeOfDay = timeOfDay,
    productId = productId,
    createdAt = createdAt?.let { parseTs(it) } ?: LocalDateTime.now(),
    syncStatus = SupplementSyncStatus.SYNCED,
    updatedAt = updatedAt?.let { parseTs(it) },
    deletedAt = deletedAt?.let { parseTs(it) },
)
