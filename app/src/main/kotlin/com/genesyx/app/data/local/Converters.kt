package com.genesyx.app.data.local

import androidx.room.TypeConverter
import com.genesyx.app.data.local.entity.LogSyncStatus
import com.genesyx.app.data.local.entity.PhSyncStatus
import java.time.LocalDate
import java.time.LocalDateTime

/** Room type converters for the value types used across entities. */
class Converters {
    // ASCII unit separator (U+001F) — will not appear in user-entered symptom/supplement text.
    private val sep = ""

    @TypeConverter
    fun fromStringList(value: List<String>?): String? = value?.joinToString(sep)

    @TypeConverter
    fun toStringList(value: String?): List<String> =
        if (value.isNullOrEmpty()) emptyList() else value.split(sep)

    @TypeConverter
    fun fromLocalDate(date: LocalDate?): Long? = date?.toEpochDay()

    @TypeConverter
    fun toLocalDate(value: Long?): LocalDate? = value?.let { LocalDate.ofEpochDay(it) }

    @TypeConverter
    fun fromLocalDateTime(dateTime: LocalDateTime?): String? = dateTime?.toString()

    @TypeConverter
    fun toLocalDateTime(value: String?): LocalDateTime? = value?.let { LocalDateTime.parse(it) }

    @TypeConverter
    fun fromPhSyncStatus(status: PhSyncStatus?): String? = status?.name

    @TypeConverter
    fun toPhSyncStatus(value: String?): PhSyncStatus =
        value?.let { runCatching { PhSyncStatus.valueOf(it) }.getOrNull() } ?: PhSyncStatus.SYNCED

    @TypeConverter
    fun fromLogSyncStatus(status: LogSyncStatus?): String? = status?.name

    @TypeConverter
    fun toLogSyncStatus(value: String?): LogSyncStatus =
        value?.let { runCatching { LogSyncStatus.valueOf(it) }.getOrNull() } ?: LogSyncStatus.SYNCED
}
