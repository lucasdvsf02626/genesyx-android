package com.genesyx.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Wire model for the Supabase `daily_logs` row (snake_case; date ISO yyyy-MM-dd; arrays as text[]). */
@Serializable
data class DailyLogDto(
    @SerialName("user_id") val userId: String,
    val date: String,
    val mood: String? = null,
    val energy: String? = null,
    val symptoms: List<String> = emptyList(),
    @SerialName("sleep_minutes") val sleepMinutes: Int? = null,
    @SerialName("water_ml") val waterMl: Int = 0,
    val supplements: List<String> = emptyList(),
    val notes: String? = null,
)
