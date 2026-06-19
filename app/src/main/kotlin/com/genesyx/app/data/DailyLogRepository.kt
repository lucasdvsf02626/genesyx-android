package com.genesyx.app.data

import com.genesyx.app.domain.model.DailyLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory daily-log store (mood/energy/symptoms/sleep/supplements/notes/water), keyed by date.
 *
 * Stands in for `getDailyLog`/`upsertDailyLog`/`getStreak` (docs/DATA_LAYER.md daily-log.functions)
 * until the remote layer lands. Hydration helpers operate on the `waterMl` field so Home, Nutrition
 * and Log all stay consistent. Streak counts consecutive days back from today with water logged.
 */
@Singleton
class DailyLogRepository @Inject constructor() {

    private val _logByDate = MutableStateFlow<Map<LocalDate, DailyLog>>(emptyMap())
    val logByDate: StateFlow<Map<LocalDate, DailyLog>> = _logByDate.asStateFlow()

    fun logOn(date: LocalDate): DailyLog = _logByDate.value[date] ?: DailyLog()

    fun waterMlOn(date: LocalDate): Int = logOn(date).waterMl

    fun upsert(date: LocalDate, log: DailyLog) {
        _logByDate.value = _logByDate.value.toMutableMap().apply { put(date, log) }
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
}
