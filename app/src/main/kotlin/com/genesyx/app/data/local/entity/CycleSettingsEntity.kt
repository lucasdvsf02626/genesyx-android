package com.genesyx.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.genesyx.app.domain.model.CycleSettings
import java.time.LocalDate

/** Room mirror of Supabase `cycle_settings` (unique per user → userId is the PK). */
@Entity(tableName = "cycle_settings")
data class CycleSettingsEntity(
    @PrimaryKey val userId: String,
    val cycleLength: Int,
    val periodLength: Int,
    val lastPeriodDate: LocalDate,
)

fun CycleSettingsEntity.toDomain(): CycleSettings =
    CycleSettings(lastPeriodDate = lastPeriodDate, cycleLength = cycleLength, periodLength = periodLength)

fun CycleSettings.toEntity(userId: String): CycleSettingsEntity =
    CycleSettingsEntity(
        userId = userId,
        cycleLength = cycleLength,
        periodLength = periodLength,
        lastPeriodDate = lastPeriodDate,
    )
