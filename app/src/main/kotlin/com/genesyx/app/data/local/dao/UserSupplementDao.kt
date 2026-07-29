package com.genesyx.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.genesyx.app.data.local.entity.SupplementSyncStatus
import com.genesyx.app.data.local.entity.UserSupplementEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface UserSupplementDao {
    /** Visible entries for a user — soft-deleted tombstones are excluded. Creation order. */
    @Query("SELECT * FROM user_supplements WHERE userId = :userId AND deletedAt IS NULL ORDER BY createdAt ASC")
    fun observeAll(userId: String): Flow<List<UserSupplementEntity>>

    @Upsert
    suspend fun upsert(entity: UserSupplementEntity)

    @Query("SELECT * FROM user_supplements WHERE id = :id")
    suspend fun getById(id: String): UserSupplementEntity?

    /** Rows with unsynced local changes (create/edit/delete) — the WorkManager queue drains these. */
    @Query("SELECT * FROM user_supplements WHERE syncStatus != 'SYNCED'")
    suspend fun pending(): List<UserSupplementEntity>

    @Query("UPDATE user_supplements SET syncStatus = :status WHERE id = :id")
    suspend fun setStatus(id: String, status: SupplementSyncStatus)

    /** Soft delete: tombstone the row and queue the delete for sync. */
    @Query("UPDATE user_supplements SET deletedAt = :deletedAt, updatedAt = :deletedAt, syncStatus = 'PENDING_DELETE' WHERE id = :id")
    suspend fun markDeleted(id: String, deletedAt: LocalDateTime)

    /**
     * Guest→account adoption on sign-in: hand every visible guest row to [userId] and queue it for
     * push. Guest tombstones are skipped — they never had a server counterpart. Returns the count.
     */
    @Query(
        "UPDATE user_supplements SET userId = :userId, syncStatus = 'PENDING_UPSERT', updatedAt = :updatedAt " +
            "WHERE userId = :guestId AND deletedAt IS NULL",
    )
    suspend fun adoptGuestRows(guestId: String, userId: String, updatedAt: LocalDateTime): Int
}
