package com.genesyx.app.ui.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.genesyx.app.data.DailyLogRepository
import com.genesyx.app.data.PhRepository
import com.genesyx.app.data.PreferencesRepository
import com.genesyx.app.data.StreakRepository
import com.genesyx.app.domain.model.Supplement
import com.genesyx.app.domain.ph.PhStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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

data class SupplementInsights(
    /** False only if her plan is empty — the card then invites her to set one rather than show 0/0. */
    val hasPlan: Boolean = true,
    val hasData: Boolean = false,
    /** Mon..Sun of the current week, as a percentage of her plan taken that day. */
    val bars: List<Int> = List(7) { 0 },
    val daysLogged: Int = 0,
    val suppTotal: Int = 0,
    val planSize: Int = Supplement.defaultPlan.size,
    val insight: String = "No supplements logged yet this week — even one, whenever you remember, is a gentle start.",
)

@HiltViewModel
class InsightsViewModel @Inject constructor(
    phRepository: PhRepository,
    dailyLogRepository: DailyLogRepository,
    streakRepository: StreakRepository,
    preferencesRepository: PreferencesRepository,
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

    // Her goal, not a constant. The bars are a percentage of what she asked for.
    val hydrationInsights: StateFlow<HydrationInsights> =
        combine(dailyLogRepository.logByDate, preferencesRepository.hydrationGoalMl) { logs, goalMl ->
            HydrationInsightLogic.compute(logs, goalMl = goalMl)
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = HydrationInsights(),
            )

    val supplementInsights: StateFlow<SupplementInsights> =
        dailyLogRepository.logByDate
            .map { logs -> SupplementInsightLogic.compute(logs) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = SupplementInsights(),
            )
}
