package com.genesyx.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.genesyx.app.data.local.entity.DailyLogEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface DailyLogDao {
    @Upsert suspend fun upsert(entity: DailyLogEntity)

    @Query("SELECT * FROM daily_logs WHERE userId = :userId AND date = :date LIMIT 1")
    fun getByUserAndDate(userId: String, date: LocalDate): Flow<DailyLogEntity?>

    @Query("SELECT * FROM daily_logs WHERE userId = :userId ORDER BY date DESC LIMIT :limit")
    fun getRecentByUser(userId: String, limit: Int = 30): Flow<List<DailyLogEntity>>

    @Query("SELECT COUNT(*) FROM daily_logs WHERE userId = :userId AND date >= :since")
    suspend fun getStreakCount(userId: String, since: LocalDate): Int
}
