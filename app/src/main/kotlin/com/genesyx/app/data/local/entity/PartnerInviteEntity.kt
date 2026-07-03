package com.genesyx.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.genesyx.app.domain.model.InviteStatus
import com.genesyx.app.domain.model.PartnerInvite

/** Room mirror of Supabase `partner_invites`, scoped to the inviter (ownerUserId). */
@Entity(tableName = "partner_invites", indices = [Index(value = ["ownerUserId"])])
data class PartnerInviteEntity(
    @PrimaryKey val id: String,
    val ownerUserId: String,
    val email: String,
    val code: String,
    val status: String,
)

fun PartnerInviteEntity.toDomain(): PartnerInvite =
    PartnerInvite(
        id = id,
        email = email,
        code = code,
        status = runCatching { InviteStatus.valueOf(status) }.getOrDefault(InviteStatus.PENDING),
    )

fun PartnerInvite.toEntity(ownerUserId: String): PartnerInviteEntity =
    PartnerInviteEntity(id = id, ownerUserId = ownerUserId, email = email, code = code, status = status.name)
