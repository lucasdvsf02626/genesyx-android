package com.genesyx.app.ui.nutrition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.genesyx.app.data.CycleRepository
import com.genesyx.app.data.DailyLogRepository
import com.genesyx.app.domain.content.FocusFood
import com.genesyx.app.domain.content.phaseFoods
import com.genesyx.app.domain.content.phaseHeroCopy
import com.genesyx.app.domain.content.phaseLabel
import com.genesyx.app.domain.cycle.CycleEngine
import com.genesyx.app.domain.model.Phase
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
    val foods: List<FocusFood> = emptyList(),
    val waterMl: Int = 0,
    val waterGoalMl: Int = 2400,
)

@HiltViewModel
class NutritionViewModel @Inject constructor(
    private val cycleRepository: CycleRepository,
    private val dailyLogRepository: DailyLogRepository,
) : ViewModel() {

    val uiState: StateFlow<NutritionUiState> =
        combine(
            cycleRepository.settings,
            dailyLogRepository.logByDate,
        ) { settings, _ ->
            val today = LocalDate.now()
            val waterMl = dailyLogRepository.waterMlOn(today)
            if (settings == null) {
                NutritionUiState(waterMl = waterMl)
            } else {
                val phase = CycleEngine.getCyclePhase(settings, today).phase
                NutritionUiState(
                    cycleSetUp = true,
                    phase = phase,
                    phaseHeader = "TODAY · ${phaseLabel.getValue(phase).uppercase()}",
                    headlineSub = phaseHeroCopy.getValue(phase).sub,
                    foods = phaseFoods.getValue(phase),
                    waterMl = waterMl,
                )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = NutritionUiState(),
        )

    fun adjustWater(deltaMl: Int) = dailyLogRepository.adjustWater(deltaMl)
}
