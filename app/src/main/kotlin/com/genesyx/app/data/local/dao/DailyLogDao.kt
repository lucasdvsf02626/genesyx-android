package com.genesyx.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.genesyx.app.data.local.entity.DailyLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyLogDao {
    @Query("SELECT * FROM daily_logs WHERE userId = :userId ORDER BY date DESC")
    fun observeAll(userId: String): Flow<List<DailyLogEntity>>

    @Upsert
    suspend fun upsert(entity: DailyLogEntity)
}
