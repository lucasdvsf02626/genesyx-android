package com.genesyx.app.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfileDto(
    val id: String,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("partner_id") val partnerId: String? = null,
    val theme: String = "dark",
)

@Serializable
data class CycleSettingsDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("cycle_length") val cycleLength: Int = 28,
    @SerialName("period_length") val periodLength: Int = 5,
    @SerialName("last_period_date") val lastPeriodDate: String,
)

@Serializable
data class DailyLogDto(
    val id: String,
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

@Serializable
data class PhReadingDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("ph_value") val phValue: Double,
    @SerialName("recorded_at") val recordedAt: String,
    val notes: String? = null,
)

@Serializable
data class PartnerInviteDto(
    val id: String,
    @SerialName("inviter_id") val inviterId: String,
    @SerialName("invitee_email") val inviteeEmail: String,
    val code: String,
    val status: String,
    @SerialName("expires_at") val expiresAt: String,
    @SerialName("accepted_by") val acceptedBy: String? = null,
    @SerialName("accepted_at") val acceptedAt: String? = null,
)
