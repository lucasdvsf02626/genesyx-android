package com.genesyx.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.genesyx.app.data.CycleRepository
import com.genesyx.app.data.DailyLogRepository
import com.genesyx.app.domain.content.phaseHeroCopy
import com.genesyx.app.domain.content.phaseLabel
import com.genesyx.app.domain.cycle.CycleEngine
import com.genesyx.app.domain.model.CycleSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

/** Home screen UI state. Mirrors the web Home (greeting, cycle hero, hydration, streak). */
data class HomeUiState(
    val userName: String = "Guest",
    val greeting: String = "Good morning",
    val settings: CycleSettings? = null,
    val cycleSetUp: Boolean = false,
    val cycleEyebrow: String = "TODAY",
    val cycleHeadline: String = "Set up your cycle",
    val cycleSub: String = "Add your last period date to get personalised insights.",
    val cycleTags: List<String> = emptyList(),
    val todayFocusTitle: String? = null,
    val todayFocusBody: String? = null,
    val hydrationLitres: Float? = null,
    val hydrationGoalLitres: Float = 2.4f,
    val streakDays: Int? = null,
    val isLoading: Boolean = false,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val cycleRepository: CycleRepository,
    private val dailyLogRepository: DailyLogRepository,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> =
        combine(
            cycleRepository.settings,
            dailyLogRepository.waterMlByDate,
        ) { settings, _ ->
            buildState(settings)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = buildState(cycleRepository.settings.value),
        )

    fun saveCycleSettings(settings: CycleSettings) = cycleRepository.upsert(settings)

    private fun buildState(settings: CycleSettings?): HomeUiState {
        val today = LocalDate.now()
        val waterMl = dailyLogRepository.waterMlOn(today)
        val base = HomeUiState(
            greeting = greetingFor(LocalTime.now()),
            settings = settings,
            hydrationLitres = if (waterMl > 0) waterMl / 1000f else null,
            streakDays = dailyLogRepository.streak(today),
        )
        if (settings == null) return base

        val info = CycleEngine.getCyclePhase(settings, today)
        val copy = phaseHeroCopy.getValue(info.phase)
        return base.copy(
            cycleSetUp = true,
            cycleEyebrow = "DAY ${info.dayOfCycle} · ${phaseLabel.getValue(info.phase).uppercase()}",
            cycleHeadline = copy.hero,
            cycleSub = copy.sub,
            cycleTags = copy.tags,
            todayFocusTitle = copy.focus.title,
            todayFocusBody = copy.focus.body,
        )
    }

    private fun greetingFor(time: LocalTime): String = when (time.hour) {
        in 5..11 -> "Good morning"
        in 12..17 -> "Good afternoon"
        else -> "Good evening"
    }
}
