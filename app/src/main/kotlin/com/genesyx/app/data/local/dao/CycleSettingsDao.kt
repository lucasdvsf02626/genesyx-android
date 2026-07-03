package com.genesyx.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.genesyx.app.data.local.entity.CycleSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CycleSettingsDao {
    @Query("SELECT * FROM cycle_settings WHERE userId = :userId LIMIT 1")
    fun observe(userId: String): Flow<CycleSettingsEntity?>

    @Upsert
    suspend fun upsert(entity: CycleSettingsEntity)

    @Query("DELETE FROM cycle_settings WHERE userId = :userId")
    suspend fun clear(userId: String)
}
