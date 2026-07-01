package com.genesyx.app.data.local

import androidx.room.TypeConverter
import java.time.Instant
import java.time.LocalDate

class Converters {
    @TypeConverter fun localDateToString(v: LocalDate?): String? = v?.toString()
    @TypeConverter fun stringToLocalDate(v: String?): LocalDate? = v?.let { LocalDate.parse(it) }

    @TypeConverter fun instantToLong(v: Instant?): Long? = v?.toEpochMilli()
    @TypeConverter fun longToInstant(v: Long?): Instant? = v?.let { Instant.ofEpochMilli(it) }

    @TypeConverter fun listToString(v: List<String>?): String? = v?.joinToString("|||")
    @TypeConverter fun stringToList(v: String?): List<String>? =
        if (v.isNullOrEmpty()) emptyList() else v.split("|||")
}
