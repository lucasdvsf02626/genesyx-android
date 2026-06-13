package com.genesyx.app.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory daily-log store for hydration + streak.
 *
 * Stands in for `getDailyLog`/`upsertDailyLog`/`getStreak` (docs/DATA_LAYER.md
 * daily-log.functions) until the remote data layer lands. Hydration is keyed by date;
 * streak counts consecutive days back from today that have any water logged, matching the
 * web `getStreak` algorithm.
 */
@Singleton
class DailyLogRepository @Inject constructor() {

    /** Daily water totals in millilitres, keyed by date. */
    private val _waterMlByDate = MutableStateFlow<Map<LocalDate, Int>>(emptyMap())
    val waterMlByDate: StateFlow<Map<LocalDate, Int>> = _waterMlByDate.asStateFlow()

    fun waterMlOn(date: LocalDate): Int = _waterMlByDate.value[date] ?: 0

    /** Adjust today's hydration by [deltaMl], clamped to 0..10000. */
    fun adjustWater(deltaMl: Int, date: LocalDate = LocalDate.now()) {
        val next = (waterMlOn(date) + deltaMl).coerceIn(0, 10_000)
        _waterMlByDate.value = _waterMlByDate.value.toMutableMap().apply { put(date, next) }
    }

    /** Consecutive days back from [today] (inclusive) that have water logged. */
    fun streak(today: LocalDate = LocalDate.now()): Int {
        val map = _waterMlByDate.value
        var streak = 0
        var day = today
        while ((map[day] ?: 0) > 0) {
            streak++
            day = day.minusDays(1)
        }
        return streak
    }
}
