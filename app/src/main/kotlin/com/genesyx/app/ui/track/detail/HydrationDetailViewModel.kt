package com.genesyx.app.ui.track.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.genesyx.app.data.DailyLogRepository
import com.genesyx.app.data.PreferencesRepository
import com.genesyx.app.data.StreakRepository
import com.genesyx.app.domain.hydration.HydrationCoach
import com.genesyx.app.domain.hydration.HydrationPace
import com.genesyx.app.domain.streaks.StreakEngine
import com.genesyx.app.ui.insights.HydrationInsightLogic
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

data class HydrationDetailState(
    val waterMl: Int = 0,
    val goalMl: Int = StreakEngine.DEFAULT_GOAL_ML,
    /** Mon..Sun of the current week, each as a percentage of the goal. */
    val bars: List<Int> = List(7) { 0 },
    val avgMlPerDay: Int? = null,
    val daysOnGoal: Int = 0,
    val streakDays: Int = 0,
    val coaching: String = "",
    val pace: HydrationPace = HydrationPace.NOT_STARTED,
    val insight: String = "",
    /** Last seven days, newest first: date → total ml. */
    val history: List<Pair<LocalDate, Int>> = emptyList(),
    val hasWeekData: Boolean = false,
)

/**
 * The canonical hydration editor's state. Reads and writes only through [DailyLogRepository] and
 * [PreferencesRepository], so Home, Nutrition, Insights, the streak engine and notification planning
 * all observe the same stored value — there is deliberately no screen-owned water total here.
 */
@HiltViewModel
class HydrationDetailViewModel @Inject constructor(
    private val dailyLogRepository: DailyLogRepository,
    private val preferencesRepository: PreferencesRepository,
    streakRepository: StreakRepository,
) : ViewModel() {

    val uiState: StateFlow<HydrationDetailState> = combine(
        dailyLogRepository.logByDate,
        preferencesRepository.hydrationGoalMl,
        streakRepository.state,
    ) { logs, goalMl, streaks ->
        val today = LocalDate.now()
        val waterToday = logs[today]?.waterMl ?: 0
        val insight = HydrationInsightLogic.compute(logs, today, goalMl)
        val coaching = HydrationCoach.coach(waterToday, goalMl, LocalTime.now())
        HydrationDetailState(
            waterMl = waterToday,
            goalMl = goalMl,
            bars = insight.bars,
            avgMlPerDay = insight.avgMlPerDay,
            daysOnGoal = streaks.daysOnGoal,
            streakDays = streaks.dailyHydration,
            coaching = coaching.message,
            pace = coaching.pace,
            insight = insight.insight,
            history = (0L until 7L).map { d ->
                val date = today.minusDays(d)
                date to (logs[date]?.waterMl ?: 0)
            },
            hasWeekData = insight.hasData,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        HydrationDetailState(),
    )

    /** Quick-add / remove. The repository clamps to a non-negative total, so a −200 can't go below 0. */
    fun add(deltaMl: Int) = dailyLogRepository.adjustWater(deltaMl)

    /** Manual entry. Coerced to a sane range so a typo can't store a negative or absurd total. */
    fun setWater(ml: Int) = dailyLogRepository.setWater(ml.coerceIn(0, 10_000))

    fun setGoal(ml: Int) = preferencesRepository.setHydrationGoalMl(ml)

    companion object {
        val GOAL_RANGE = StreakEngine.GOAL_RANGE_ML
        const val GOAL_STEP = StreakEngine.GOAL_STEP_ML
    }
}
