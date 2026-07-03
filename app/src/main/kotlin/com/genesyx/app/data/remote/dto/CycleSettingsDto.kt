package com.genesyx.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Wire model for the Supabase `cycle_settings` row (snake_case; date as ISO yyyy-MM-dd). */
@Serializable
data class CycleSettingsDto(
    @SerialName("user_id") val userId: String,
    @SerialName("cycle_length") val cycleLength: Int,
    @SerialName("period_length") val periodLength: Int,
    @SerialName("last_period_date") val lastPeriodDate: String,
)
