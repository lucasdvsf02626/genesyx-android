package com.genesyx.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.genesyx.app.data.local.entity.PhReadingEntity
import com.genesyx.app.data.local.entity.PhSyncStatus
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface PhReadingDao {
    /** Visible readings for a user — soft-deleted tombstones are excluded. */
    @Query("SELECT * FROM ph_readings WHERE userId = :userId AND deletedAt IS NULL ORDER BY recordedAt ASC")
    fun observeAll(userId: String): Flow<List<PhReadingEntity>>

    @Upsert
    suspend fun upsert(entity: PhReadingEntity)

    @Query("SELECT * FROM ph_readings WHERE id = :id")
    suspend fun getById(id: String): PhReadingEntity?

    /** Rows with unsynced local changes (create/edit/delete) — the WorkManager queue drains these. */
    @Query("SELECT * FROM ph_readings WHERE syncStatus != 'SYNCED'")
    suspend fun pending(): List<PhReadingEntity>

    @Query("UPDATE ph_readings SET syncStatus = :status WHERE id = :id")
    suspend fun setStatus(id: String, status: PhSyncStatus)

    /** Soft delete: tombstone the row and queue the delete for sync. */
    @Query("UPDATE ph_readings SET deletedAt = :deletedAt, updatedAt = :deletedAt, syncStatus = 'PENDING_DELETE' WHERE id = :id")
    suspend fun markDeleted(id: String, deletedAt: LocalDateTime)
}
