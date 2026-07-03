package com.genesyx.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.genesyx.app.domain.model.Client
import com.genesyx.app.domain.model.ClientStatus

/**
 * A client/managed record owned by an authenticated user. This is the scaling seam: an account
 * (coach / clinician / power user) can hold many client records, each isolated by [ownerUserId]
 * (the RLS scope key when synced to Supabase). Indexed by owner for fast, paginated listing.
 */
@Entity(tableName = "clients", indices = [Index(value = ["ownerUserId"])])
data class ClientEntity(
    @PrimaryKey val id: String,
    val ownerUserId: String,
    val displayName: String,
    val email: String?,
    val notes: String?,
    val status: String,
    val createdAt: Long,
    val updatedAt: Long,
)

fun ClientEntity.toDomain(): Client =
    Client(
        id = id,
        ownerUserId = ownerUserId,
        displayName = displayName,
        email = email,
        notes = notes,
        status = runCatching { ClientStatus.valueOf(status) }.getOrDefault(ClientStatus.ACTIVE),
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun Client.toEntity(): ClientEntity =
    ClientEntity(
        id = id,
        ownerUserId = ownerUserId,
        displayName = displayName,
        email = email,
        notes = notes,
        status = status.name,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
