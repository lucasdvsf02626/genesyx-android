package com.genesyx.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.genesyx.app.domain.consent.ConsentAction
import com.genesyx.app.domain.consent.ConsentEvent
import java.time.LocalDateTime

/**
 * Room mirror of one consent event, synced with the Supabase `consent_events` table on sign-in
 * (ConsentRepository.refresh — pull-merge-push, append-only both ways) and scoped by [userId] like
 * every other table so accounts stay isolated on a shared device.
 *
 * There is deliberately no sync-status column and no "current state" column: the row is a fact that
 * happened at a point in time, and the only way to learn whether collection is permitted is to read
 * the newest row. Index (userId, recordedAt) matches that read.
 */
@Entity(tableName = "consent_events", indices = [Index(value = ["userId", "recordedAt"])])
data class ConsentEventEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val action: String,
    val recordedAt: LocalDateTime,
)

fun ConsentEventEntity.toDomain(): ConsentEvent = ConsentEvent(
    id = id,
    action = ConsentAction.fromWire(action),
    recordedAt = recordedAt,
)

fun ConsentEvent.toEntity(userId: String): ConsentEventEntity = ConsentEventEntity(
    id = id,
    userId = userId,
    action = action.wire,
    recordedAt = recordedAt,
)
