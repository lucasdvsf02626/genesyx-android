package com.genesyx.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.genesyx.app.data.local.entity.PhReadingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PhReadingDao {
    @Query("SELECT * FROM ph_readings WHERE userId = :userId ORDER BY recordedAt ASC")
    fun observeAll(userId: String): Flow<List<PhReadingEntity>>

    @Upsert
    suspend fun upsert(entity: PhReadingEntity)

    @Query("DELETE FROM ph_readings WHERE id = :id")
    suspend fun delete(id: String)
}
