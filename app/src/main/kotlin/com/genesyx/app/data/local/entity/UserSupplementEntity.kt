package com.genesyx.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.genesyx.app.domain.model.SupplementTime
import com.genesyx.app.domain.model.UserSupplement
import java.time.LocalDateTime

/**
 * Room mirror of Supabase `user_supplements` — index (userId, createdAt) matches the server index.
 * Same offline-sync bookkeeping as [PhReadingEntity]: [syncStatus], [updatedAt] (last-write-wins
 * clock) and [deletedAt] (soft delete / tombstone) are local-only concerns, NOT surfaced in the
 * [UserSupplement] domain model.
 */
@Entity(tableName = "user_supplements", indices = [Index(value = ["userId", "createdAt"])])
data class UserSupplementEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val dose: String?,
    /** Wire value of [SupplementTime]; null when the user didn't pick one. */
    val timeOfDay: String?,
    val productId: String?,
    val createdAt: LocalDateTime,
    @ColumnInfo(defaultValue = "SYNCED") val syncStatus: SupplementSyncStatus = SupplementSyncStatus.SYNCED,
    val updatedAt: LocalDateTime? = null,
    val deletedAt: LocalDateTime? = null,
)

fun UserSupplementEntity.toDomain(): UserSupplement = UserSupplement(
    id = id,
    name = name,
    dose = dose,
    timeOfDay = SupplementTime.fromWire(timeOfDay),
    productId = productId,
)

fun UserSupplement.toEntity(
    userId: String,
    createdAt: LocalDateTime,
    syncStatus: SupplementSyncStatus = SupplementSyncStatus.SYNCED,
    updatedAt: LocalDateTime? = null,
    deletedAt: LocalDateTime? = null,
): UserSupplementEntity = UserSupplementEntity(
    id = id,
    userId = userId,
    name = name,
    dose = dose,
    timeOfDay = timeOfDay?.wire,
    productId = productId,
    createdAt = createdAt,
    syncStatus = syncStatus,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)
