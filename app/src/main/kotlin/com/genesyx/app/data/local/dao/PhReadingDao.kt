package com.genesyx.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.genesyx.app.data.local.entity.PhReadingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PhReadingDao {
    @Upsert suspend fun upsert(entity: PhReadingEntity)

    @Query("SELECT * FROM ph_readings WHERE userId = :userId ORDER BY recordedAt DESC LIMIT :limit")
    fun getRecentByUser(userId: String, limit: Int = 20): Flow<List<PhReadingEntity>>
}
