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

    @Query("UPDATE daily_logs SET syncStatus = :status WHERE userId = :userId AND date = :date")
    suspend fun setStatus(userId: String, date: LocalDate, status: LogSyncStatus)
}
