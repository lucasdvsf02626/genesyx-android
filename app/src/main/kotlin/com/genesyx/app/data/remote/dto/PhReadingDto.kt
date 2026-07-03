package com.genesyx.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Wire model for the Supabase `ph_readings` row (snake_case; recorded_at ISO timestamp). */
@Serializable
data class PhReadingDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("ph_value") val phValue: Double,
    @SerialName("recorded_at") val recordedAt: String,
    val notes: String? = null,
)
