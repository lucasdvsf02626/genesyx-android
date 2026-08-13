package com.genesyx.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.genesyx.app.data.local.entity.MealEntryEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface MealEntryDao {
    /** A day's meals for a user, newest-logged first. */
    @Query("SELECT * FROM meal_entries WHERE userId = :userId AND date = :date ORDER BY loggedAt DESC")
    fun observeForDate(userId: String, date: LocalDate): Flow<List<MealEntryEntity>>

    @Upsert
    suspend fun upsert(entity: MealEntryEntity)

    @Query("DELETE FROM meal_entries WHERE id = :id")
    suspend fun delete(id: String)
}
