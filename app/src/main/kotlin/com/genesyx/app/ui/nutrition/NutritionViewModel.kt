package com.genesyx.app.ui.nutrition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.genesyx.app.data.CycleRepository
import com.genesyx.app.data.DailyLogRepository
import com.genesyx.app.data.PreferencesRepository
import com.genesyx.app.data.StreakRepository
import com.genesyx.app.domain.content.PhaseFood
import com.genesyx.app.domain.content.nutritionPhaseDescription
import com.genesyx.app.domain.content.nutritionPhaseFoods
import com.genesyx.app.domain.content.phaseLabel
import com.genesyx.app.domain.cycle.CycleEngine
import com.genesyx.app.domain.model.Phase
import com.genesyx.app.domain.streaks.StreakEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

data class NutritionUiState(
    val cycleSetUp: Boolean = false,
    val phase: Phase? = null,
    val phaseHeader: String = "TODAY · SET UP YOUR CYCLE",
    val headlineSub: String = "Set up your cycle to get personalised nutrition guidance.",
    val foods: List<PhaseFood> = emptyList(),
    val waterMl: Int = 0,
    /** Her goal, from preferences — [StreakEngine.DEFAULT_GOAL_ML] only until she sets her own. */
    val waterGoalMl: Int = StreakEngine.DEFAULT_GOAL_ML,
    val weeklyStreak: Int = 0,
    /** Days this week she actually hit [waterGoalMl], which is not the same as days she logged. */
    val daysOnGoal: Int = 0,
)

@HiltViewModel
class NutritionViewModel @Inject constructor(
    private val cycleRepository: CycleRepository,
    private val dailyLogRepository: DailyLogRepository,
    private val preferencesRepository: PreferencesRepository,
    streakRepository: StreakRepository,
) : ViewModel() {

    val uiState: StateFlow<NutritionUiState> =
        combine(
            cycleRepository.settings,
            dailyLogRepository.logByDate,
            streakRepository.state,
            preferencesRepository.hydrationGoalMl,
        ) { settings, _, streaks, goalMl ->
            val today = LocalDate.now()
            val waterMl = dailyLogRepository.waterMlOn(today)
            if (settings == null) {
                NutritionUiState(
                    waterMl = waterMl,
                    waterGoalMl = goalMl,
                    weeklyStreak = streaks.weeklyStreak,
                    daysOnGoal = streaks.daysOnGoal,
                )
            } else {
                val phase = CycleEngine.getCyclePhase(settings, today).phase
                NutritionUiState(
                    cycleSetUp = true,
                    phase = phase,
                    phaseHeader = "TODAY · ${phaseLabel.getValue(phase).uppercase()}",
                    headlineSub = nutritionPhaseDescription.getValue(phase),
                    foods = nutritionPhaseFoods.getValue(phase),
                    waterMl = waterMl,
                    waterGoalMl = goalMl,
                    weeklyStreak = streaks.weeklyStreak,
                    daysOnGoal = streaks.daysOnGoal,
                )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = NutritionUiState(),
        )

    fun adjustWater(deltaMl: Int) = dailyLogRepository.adjustWater(deltaMl)

    fun setWaterGoal(goalMl: Int) = preferencesRepository.setHydrationGoalMl(goalMl)
}
