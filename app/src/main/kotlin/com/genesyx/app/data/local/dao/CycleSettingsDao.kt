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

    /** The row read once — for the owed-push drain and guest adoption, which must not race a flow. */
    @Query("SELECT * FROM cycle_settings WHERE userId = :userId LIMIT 1")
    suspend fun get(userId: String): CycleSettingsEntity?

    @Upsert
    suspend fun upsert(entity: CycleSettingsEntity)

    @Query("DELETE FROM cycle_settings WHERE userId = :userId")
    suspend fun clear(userId: String)
}
