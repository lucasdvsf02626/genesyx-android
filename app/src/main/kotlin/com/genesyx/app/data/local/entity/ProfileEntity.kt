package com.genesyx.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room mirror of Supabase `profiles`. Kept in the DB so profile data can be cached/synced per user
 * (display name, avatar, partner link, theme). Active display name/email currently live in the
 * DataStore session mirror; this is the sync-ready home for the full profile row.
 */
@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey val id: String,
    val displayName: String?,
    val avatarUrl: String?,
    val partnerId: String?,
    val theme: String,
)
