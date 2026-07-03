package com.genesyx.app.data.local.entity

import androidx.room.Entity
import com.genesyx.app.domain.model.DailyLog
import com.genesyx.app.domain.model.EnergyLevel
import com.genesyx.app.domain.model.Mood
import java.time.LocalDate

/** Room mirror of Supabase `daily_logs` — UNIQUE(user_id, date) → composite primary key. */
@Entity(tableName = "daily_logs", primaryKeys = ["userId", "date"])
data class DailyLogEntity(
    val userId: String,
    val date: LocalDate,
    val moodId: String?,
    val energyId: String?,
    val symptoms: List<String>,
    val sleepMinutes: Int?,
    val supplements: List<String>,
    val notes: String?,
    val waterMl: Int,
)

fun DailyLogEntity.toDomain(): DailyLog =
    DailyLog(
        mood = moodId?.let { id -> Mood.entries.firstOrNull { it.id == id } },
        energy = energyId?.let { id -> EnergyLevel.entries.firstOrNull { it.id == id } },
        symptoms = symptoms.toSet(),
        sleepMinutes = sleepMinutes,
        supplements = supplements.toSet(),
        notes = notes,
        waterMl = waterMl,
    )

fun DailyLog.toEntity(userId: String, date: LocalDate): DailyLogEntity =
    DailyLogEntity(
        userId = userId,
        date = date,
        moodId = mood?.id,
        energyId = energy?.id,
        symptoms = symptoms.toList(),
        sleepMinutes = sleepMinutes,
        supplements = supplements.toList(),
        notes = notes,
        waterMl = waterMl,
    )
