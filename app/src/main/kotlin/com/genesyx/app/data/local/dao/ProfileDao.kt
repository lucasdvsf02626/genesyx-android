package com.genesyx.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.genesyx.app.data.local.entity.ProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Upsert suspend fun upsert(entity: ProfileEntity)
    @Query("SELECT * FROM profiles WHERE id = :id") fun getById(id: String): Flow<ProfileEntity?>
}
