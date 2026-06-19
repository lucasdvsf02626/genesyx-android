package com.genesyx.app.data.local

import com.genesyx.app.domain.model.CycleSettings
import com.genesyx.app.domain.model.DailyLog
import com.genesyx.app.domain.model.EnergyLevel
import com.genesyx.app.domain.model.Mood
import com.genesyx.app.domain.model.PhReading
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.LocalDateTime

// Serializable persistence DTOs. java.time values are stored as ISO-8601 strings to avoid
// custom serializers; enums persist by name. Mappers convert to/from the domain models.

@Serializable
data class CycleSettingsDto(val lastPeriodDate: String, val cycleLength: Int, val periodLength: Int)

fun CycleSettings.toDto() = CycleSettingsDto(lastPeriodDate.toString(), cycleLength, periodLength)
fun CycleSettingsDto.toDomain() = CycleSettings(LocalDate.parse(lastPeriodDate), cycleLength, periodLength)

@Serializable
data class DailyLogDto(
    val mood: String? = null,
    val energy: String? = null,
    val symptoms: List<String> = emptyList(),
    val sleepMinutes: Int? = null,
    val supplements: List<String> = emptyList(),
    val notes: String? = null,
    val waterMl: Int = 0,
)

fun DailyLog.toDto() = DailyLogDto(
    mood = mood?.name,
    energy = energy?.name,
    symptoms = symptoms.toList(),
    sleepMinutes = sleepMinutes,
    supplements = supplements.toList(),
    notes = notes,
    waterMl = waterMl,
)

fun DailyLogDto.toDomain() = DailyLog(
    mood = mood?.let { runCatching { Mood.valueOf(it) }.getOrNull() },
    energy = energy?.let { runCatching { EnergyLevel.valueOf(it) }.getOrNull() },
    symptoms = symptoms.toSet(),
    sleepMinutes = sleepMinutes,
    supplements = supplements.toSet(),
    notes = notes,
    waterMl = waterMl,
)

@Serializable
data class PhReadingDto(val id: String, val phValue: Double, val recordedAt: String, val notes: String? = null)

fun PhReading.toDto() = PhReadingDto(id, phValue, recordedAt.toString(), notes)
fun PhReadingDto.toDomain() = PhReading(id, phValue, LocalDateTime.parse(recordedAt), notes)
