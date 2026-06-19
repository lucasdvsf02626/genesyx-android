package com.genesyx.app.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.genesyx.app.data.local.DailyLogDto
import com.genesyx.app.data.local.toDomain
import com.genesyx.app.data.local.toDto
import com.genesyx.app.domain.model.DailyLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Daily logs (mood/energy/symptoms/sleep/supplements/notes/water), persisted on-device via
 * DataStore as a date-keyed JSON map. Hydration helpers operate on the `waterMl` field so Home,
 * Nutrition and Log stay consistent; streak counts consecutive days back from today with water.
 */
@Singleton
class DailyLogRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val json: Json,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val key = stringPreferencesKey("daily_logs")

    private val _logByDate = MutableStateFlow<Map<LocalDate, DailyLog>>(emptyMap())
    val logByDate: StateFlow<Map<LocalDate, DailyLog>> = _logByDate.asStateFlow()

    init {
        scope.launch {
            dataStore.data.first()[key]?.let { raw ->
                runCatching { json.decodeFromString<Map<String, DailyLogDto>>(raw) }
                    .getOrNull()?.let { stored ->
                        _logByDate.value = stored.entries.associate { (k, v) -> LocalDate.parse(k) to v.toDomain() }
                    }
            }
        }
    }

    fun logOn(date: LocalDate): DailyLog = _logByDate.value[date] ?: DailyLog()

    fun waterMlOn(date: LocalDate): Int = logOn(date).waterMl

    fun upsert(date: LocalDate, log: DailyLog) {
        _logByDate.value = _logByDate.value.toMutableMap().apply { put(date, log) }
        persist()
    }

    /** Adjust today's hydration by [deltaMl], clamped to 0..10000. */
    fun adjustWater(deltaMl: Int, date: LocalDate = LocalDate.now()) {
        val next = (waterMlOn(date) + deltaMl).coerceIn(0, 10_000)
        upsert(date, logOn(date).copy(waterMl = next))
    }

    /** Set today's hydration to [ml], clamped to 0..10000. */
    fun setWater(ml: Int, date: LocalDate = LocalDate.now()) {
        upsert(date, logOn(date).copy(waterMl = ml.coerceIn(0, 10_000)))
    }

    /** Consecutive days back from [today] (inclusive) that have water logged. */
    fun streak(today: LocalDate = LocalDate.now()): Int {
        val map = _logByDate.value
        var streak = 0
        var day = today
        while ((map[day]?.waterMl ?: 0) > 0) {
            streak++
            day = day.minusDays(1)
        }
        return streak
    }

    private fun persist() {
        val snapshot = _logByDate.value.entries.associate { (k, v) -> k.toString() to v.toDto() }
        scope.launch { dataStore.edit { it[key] = json.encodeToString(snapshot) } }
    }
}
