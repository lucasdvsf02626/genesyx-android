package com.genesyx.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey val id: String,
    val displayName: String?,
    val avatarUrl: String?,
    val partnerId: String?,
    val theme: String = "dark",
)

@Entity(tableName = "cycle_settings")
data class CycleSettingsEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val cycleLength: Int = 28,
    val periodLength: Int = 5,
    val lastPeriodDate: LocalDate,
)

@Entity(tableName = "daily_logs")
data class DailyLogEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val date: LocalDate,
    val mood: String?,
    val energy: String?,
    val symptoms: List<String> = emptyList(),
    val sleepMinutes: Int?,
    val waterMl: Int = 0,
    val supplements: List<String> = emptyList(),
    val notes: String?,
)

@Entity(tableName = "ph_readings")
data class PhReadingEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val phValue: Double,
    val recordedAt: Instant,
    val notes: String?,
)

@Entity(tableName = "partner_invites")
data class PartnerInviteEntity(
    @PrimaryKey val id: String,
    val inviterId: String,
    val inviteeEmail: String,
    val code: String,
    val status: String,
    val expiresAt: Instant,
    val acceptedBy: String?,
    val acceptedAt: Instant?,
)
