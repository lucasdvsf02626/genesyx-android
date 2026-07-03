package com.genesyx.app.data.local

import androidx.room.TypeConverter
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
}
