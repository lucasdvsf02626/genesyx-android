package com.genesyx.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.genesyx.app.data.local.entity.DailyLogEntity
import com.genesyx.app.data.local.entity.LogSyncStatus
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface DailyLogDao {
    @Query("SELECT * FROM daily_logs WHERE userId = :userId ORDER BY date DESC")
    fun observeAll(userId: String): Flow<List<DailyLogEntity>>

    @Upsert
    suspend fun upsert(entity: DailyLogEntity)

    @Query("SELECT * FROM daily_logs WHERE userId = :userId AND date = :date")
    suspend fun getByDate(userId: String, date: LocalDate): DailyLogEntity?

    /** Rows written locally but not yet pushed — the WorkManager queue drains these. */
    @Query("SELECT * FROM daily_logs WHERE syncStatus != 'SYNCED'")
    suspend fun pending(): List<DailyLogEntity>

    /** Live count of unsynced rows — powers the Profile sync-status row. */
    @Query("SELECT COUNT(*) FROM daily_logs WHERE userId = :userId AND syncStatus != 'SYNCED'")
    fun observePendingCount(userId: String): Flow<Int>

    @Query("UPDATE daily_logs SET syncStatus = :status WHERE userId = :userId AND date = :date")
    suspend fun setStatus(userId: String, date: LocalDate, status: LogSyncStatus)

    /**
     * Re-keys the guest bucket's logs onto the account at sign-in, marked PENDING_UPSERT so the
     * ordinary queue pushes them. The PK is (userId, date), so a date the account already holds
     * locally is skipped rather than collided with — in practice sign-out's clearAllTables means
     * there rarely is one. Returns the number of rows adopted.
     */
    @Query(
        "UPDATE daily_logs SET userId = :userId, syncStatus = 'PENDING_UPSERT' " +
            "WHERE userId = :guestId AND date NOT IN (SELECT date FROM daily_logs WHERE userId = :userId)",
    )
    suspend fun adoptGuestRows(guestId: String, userId: String): Int
}
