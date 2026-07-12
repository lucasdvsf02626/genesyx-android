package com.genesyx.app.ui.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.genesyx.app.data.DailyLogRepository
import com.genesyx.app.data.PhRepository
import com.genesyx.app.data.StreakRepository
import com.genesyx.app.domain.ph.PhStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

enum class Trend { UP, DOWN, FLAT }

data class PhInsights(
    val hasReadings: Boolean = false,
    val currentValue: Double? = null,
    val currentStatus: PhStatus? = null,
    val trend: Trend = Trend.FLAT,
    val avg7: Double? = null,
    val avg30: Double? = null,
    val readings30: Int = 0,
    val insight: String = "Log a few pH readings to see personalised insights here.",
    val recommendation: String = "",
)

data class ConsistencyInsights(
    val dailyStreak: Int = 0,
    val weeklyStreak: Int = 0,
    val daysLoggedThisWeek: Int = 0,
    val weekActivity: List<Boolean> = List(7) { false },
    val bestDailyStreak: Int = 0,
    val insight: String = "Log anything at all — water, a mood, a symptom — and this week starts counting.",
)

data class HydrationInsights(
    val hasData: Boolean = false,
    /** Mon..Sun of the current week, as a percentage of the daily goal. */
    val bars: List<Int> = List(7) { 0 },
    val avgMlPerDay: Int? = null,
    val deltaMlPerDay: Int? = null,
    val insight: String = "Log your water for a few days and your hydration pattern will show up here.",
)

@HiltViewModel
class InsightsViewModel @Inject constructor(
    phRepository: PhRepository,
    dailyLogRepository: DailyLogRepository,
    streakRepository: StreakRepository,
) : ViewModel() {

    val phInsights: StateFlow<PhInsights> =
        phRepository.readings
            .map { readings -> PhInsightLogic.compute(readings) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = PhInsights(),
            )

    val consistencyInsights: StateFlow<ConsistencyInsights> =
        streakRepository.state
            .map { streaks -> ConsistencyInsightLogic.compute(streaks) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = ConsistencyInsights(),
            )

    val hydrationInsights: StateFlow<HydrationInsights> =
        dailyLogRepository.logByDate
            .map { logs -> HydrationInsightLogic.compute(logs) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = HydrationInsights(),
            )
}
