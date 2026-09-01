package com.genesyx.app.data.remote.dto

import kotlinx.serialization.Serializable

/** Wire model for a Supabase `app_config` row — key/value server configuration. */
@Serializable
data class AppConfigEntryDto(
    val key: String,
    val value: String,
)
