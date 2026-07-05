package com.genesyx.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.genesyx.app.domain.model.PhReading
import java.time.LocalDateTime

/**
 * Room mirror of Supabase `ph_readings` — index (userId, recordedAt) matches the server index.
 * v3 adds offline-sync bookkeeping: [syncStatus], [updatedAt] (last-write-wins clock) and
 * [deletedAt] (soft delete / tombstone). These are local-only concerns and are NOT surfaced in the
 * [PhReading] domain model.
 */
@Entity(tableName = "ph_readings", indices = [Index(value = ["userId", "recordedAt"])])
data class PhReadingEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val phValue: Double,
    val recordedAt: LocalDateTime,
    val notes: String?,
    @ColumnInfo(defaultValue = "SYNCED") val syncStatus: PhSyncStatus = PhSyncStatus.SYNCED,
    val updatedAt: LocalDateTime? = null,
    val deletedAt: LocalDateTime? = null,
)

fun PhReadingEntity.toDomain(): PhReading =
    PhReading(id = id, phValue = phValue, recordedAt = recordedAt, notes = notes)

fun PhReading.toEntity(
    userId: String,
    syncStatus: PhSyncStatus = PhSyncStatus.SYNCED,
    updatedAt: LocalDateTime? = null,
    deletedAt: LocalDateTime? = null,
): PhReadingEntity = PhReadingEntity(
    id = id,
    userId = userId,
    phValue = phValue,
    recordedAt = recordedAt,
    notes = notes,
    syncStatus = syncStatus,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)
