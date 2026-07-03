package com.genesyx.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** The current user's linked partner (one per owner). Mirrors the `profiles.partner_id` link. */
@Entity(tableName = "partner_links")
data class PartnerLinkEntity(
    @PrimaryKey val ownerUserId: String,
    val partnerName: String,
)
