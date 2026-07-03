package com.genesyx.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.genesyx.app.domain.model.PhReading
import java.time.LocalDateTime

/** Room mirror of Supabase `ph_readings` — index (userId, recordedAt) matches the server index. */
@Entity(tableName = "ph_readings", indices = [Index(value = ["userId", "recordedAt"])])
data class PhReadingEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val phValue: Double,
    val recordedAt: LocalDateTime,
    val notes: String?,
)

fun PhReadingEntity.toDomain(): PhReading =
    PhReading(id = id, phValue = phValue, recordedAt = recordedAt, notes = notes)

fun PhReading.toEntity(userId: String): PhReadingEntity =
    PhReadingEntity(id = id, userId = userId, phValue = phValue, recordedAt = recordedAt, notes = notes)
