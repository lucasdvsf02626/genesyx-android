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
    /**
     * Omitted from the wire while null (encodeDefaults is off in the shared serializer), so a log
     * without an intimacy record still syncs against a server that predates the column — only a
     * row that actually carries one needs the migration applied, and until then it queues and
     * retries like any failed push.
     */
    @SerialName("sexual_activity") val sexualActivity: Boolean? = null,
)
