package com.genesyx.app.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.genesyx.app.notifications.model.NotificationSettings
import com.genesyx.app.notifications.model.ReminderKind
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DataStore-backed store for [NotificationSettings]. Sits beside [PreferencesRepository] and shares
 * the one `genesyx_prefs` DataStore, reusing the existing `push_enabled` key as the master switch so
 * the reminder engine and the Profile toggle never disagree about it.
 *
 * The trailing counters (daily cap, re-engagement pacing) are written by the engine, not the user.
 */
@Singleton
class NotificationSettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    private object Keys {
        val MASTER = booleanPreferencesKey("push_enabled") // shared with the Profile master switch
        val ENABLED_KINDS = stringSetPreferencesKey("notif_enabled_kinds")
        val DAILY_TIME = intPreferencesKey("notif_daily_time_min")
        val DAILY_DAYS = stringSetPreferencesKey("notif_daily_days")
        val WEEKLY_TIME = intPreferencesKey("notif_weekly_time_min")
        val WEEKLY_DAY = stringPreferencesKey("notif_weekly_day")
        val HYDRATION_TIME = intPreferencesKey("notif_hydration_time_min")
        val QUIET_ENABLED = booleanPreferencesKey("notif_quiet_enabled")
        val QUIET_START = intPreferencesKey("notif_quiet_start_min")
        val QUIET_END = intPreferencesKey("notif_quiet_end_min")
        val LAST_PROMPTED = longPreferencesKey("notif_last_prompted_at")
        val FIRST_GRANTED = longPreferencesKey("notif_first_granted_at")
        val POSTED_TODAY = intPreferencesKey("notif_posted_today")
        val POSTED_EPOCHDAY = longPreferencesKey("notif_posted_epochday")
        val LAST_REENGAGE = longPreferencesKey("notif_last_reengage_epochday")
        val CONSEC_IGNORED = intPreferencesKey("notif_consec_ignored_reengage")
        val LAST_OPENED = longPreferencesKey("notif_last_opened_epochday")
        val LAST_MISSED = longPreferencesKey("notif_last_missed_epochday")
    }

    val settings: Flow<NotificationSettings> = dataStore.data.map { p ->
        val defaults = NotificationSettings()
        NotificationSettings(
            masterEnabled = p[Keys.MASTER] ?: defaults.masterEnabled,
            enabledKinds = p[Keys.ENABLED_KINDS]?.mapNotNull(::kindOrNull)?.toSet() ?: defaults.enabledKinds,
            dailyLogTime = p[Keys.DAILY_TIME]?.let(::time) ?: defaults.dailyLogTime,
            dailyLogDays = p[Keys.DAILY_DAYS]?.mapNotNull(::dayOrNull)?.toSet() ?: defaults.dailyLogDays,
            weeklyInsightsTime = p[Keys.WEEKLY_TIME]?.let(::time) ?: defaults.weeklyInsightsTime,
            weeklyInsightsDay = p[Keys.WEEKLY_DAY]?.let(::dayOrNull) ?: defaults.weeklyInsightsDay,
            hydrationTime = p[Keys.HYDRATION_TIME]?.let(::time) ?: defaults.hydrationTime,
            quietHoursEnabled = p[Keys.QUIET_ENABLED] ?: defaults.quietHoursEnabled,
            quietHoursStart = p[Keys.QUIET_START]?.let(::time) ?: defaults.quietHoursStart,
            quietHoursEnd = p[Keys.QUIET_END]?.let(::time) ?: defaults.quietHoursEnd,
            lastPromptedAt = p[Keys.LAST_PROMPTED],
            firstGrantedAt = p[Keys.FIRST_GRANTED],
            notificationsPostedToday = p[Keys.POSTED_TODAY] ?: 0,
            postedCounterEpochDay = p[Keys.POSTED_EPOCHDAY] ?: 0,
            lastReengagementEpochDay = p[Keys.LAST_REENGAGE],
            consecutiveIgnoredReengagements = p[Keys.CONSEC_IGNORED] ?: 0,
            lastOpenedEpochDay = p[Keys.LAST_OPENED],
            lastMissedLogEpochDay = p[Keys.LAST_MISSED],
        )
    }

    suspend fun current(): NotificationSettings = settings.first()

    suspend fun setMasterEnabled(enabled: Boolean) = dataStore.edit { it[Keys.MASTER] = enabled }.let {}

    suspend fun setKindEnabled(kind: ReminderKind, enabled: Boolean) = dataStore.edit { p ->
        val current = p[Keys.ENABLED_KINDS]?.mapNotNull(::kindOrNull)?.toMutableSet()
            ?: NotificationSettings().enabledKinds.toMutableSet()
        if (enabled) current.add(kind) else current.remove(kind)
        p[Keys.ENABLED_KINDS] = current.map { it.name }.toSet()
    }.let {}

    suspend fun setDailyTime(time: LocalTime) = dataStore.edit { it[Keys.DAILY_TIME] = minutes(time) }.let {}
    suspend fun setDailyDays(days: Set<DayOfWeek>) = dataStore.edit { it[Keys.DAILY_DAYS] = days.map { d -> d.name }.toSet() }.let {}
    suspend fun setWeeklyTime(time: LocalTime) = dataStore.edit { it[Keys.WEEKLY_TIME] = minutes(time) }.let {}
    suspend fun setWeeklyDay(day: DayOfWeek) = dataStore.edit { it[Keys.WEEKLY_DAY] = day.name }.let {}
    suspend fun setHydrationTime(time: LocalTime) = dataStore.edit { it[Keys.HYDRATION_TIME] = minutes(time) }.let {}

    suspend fun setQuietHours(enabled: Boolean, start: LocalTime, end: LocalTime) = dataStore.edit {
        it[Keys.QUIET_ENABLED] = enabled
        it[Keys.QUIET_START] = minutes(start)
        it[Keys.QUIET_END] = minutes(end)
    }.let {}

    suspend fun setPrompted(atMillis: Long) = dataStore.edit { it[Keys.LAST_PROMPTED] = atMillis }.let {}

    suspend fun recordFirstGrant(atMillis: Long) = dataStore.edit {
        if (it[Keys.FIRST_GRANTED] == null) it[Keys.FIRST_GRANTED] = atMillis
    }.let {}

    /** Bumps the daily cap counter, resetting it when the stored day is not today. */
    suspend fun recordPosted(todayEpochDay: Long) = dataStore.edit { p ->
        val sameDay = p[Keys.POSTED_EPOCHDAY] == todayEpochDay
        p[Keys.POSTED_TODAY] = (if (sameDay) (p[Keys.POSTED_TODAY] ?: 0) else 0) + 1
        p[Keys.POSTED_EPOCHDAY] = todayEpochDay
    }.let {}

    /** A posted re-engagement is treated as ignored until the app is next opened. */
    suspend fun recordReengagementPosted(todayEpochDay: Long) = dataStore.edit { p ->
        p[Keys.LAST_REENGAGE] = todayEpochDay
        p[Keys.CONSEC_IGNORED] = (p[Keys.CONSEC_IGNORED] ?: 0) + 1
    }.let {}

    suspend fun recordMissedLog(todayEpochDay: Long) = dataStore.edit { it[Keys.LAST_MISSED] = todayEpochDay }.let {}

    /** App opened: record the day and clear the re-engagement "ignored" streak. */
    suspend fun markOpened(todayEpochDay: Long) = dataStore.edit {
        it[Keys.LAST_OPENED] = todayEpochDay
        it[Keys.CONSEC_IGNORED] = 0
    }.let {}

    private fun minutes(t: LocalTime): Int = t.hour * 60 + t.minute
    private fun time(min: Int): LocalTime = LocalTime.of((min / 60) % 24, min % 60)
    private fun kindOrNull(name: String): ReminderKind? = runCatching { ReminderKind.valueOf(name) }.getOrNull()
    private fun dayOrNull(name: String): DayOfWeek? = runCatching { DayOfWeek.valueOf(name) }.getOrNull()
}
