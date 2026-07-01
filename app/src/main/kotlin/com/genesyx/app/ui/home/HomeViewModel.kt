package com.genesyx.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.genesyx.app.data.preferences.OnboardingPreferences
import com.genesyx.app.data.repository.AuthRepository
import com.genesyx.app.data.repository.CycleRepository
import com.genesyx.app.data.repository.DailyLogRepository
import com.genesyx.app.data.repository.ProfileRepository
import com.genesyx.app.domain.content.phaseFoods
import com.genesyx.app.domain.content.phaseHeroCopy
import com.genesyx.app.domain.cycle.CycleEngine
import com.genesyx.app.domain.model.CycleSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class HomeUiState(
    val userName: String = "",
    val cycleSetUp: Boolean = false,
    val cycleEyebrow: String = "TODAY",
    val cycleHeadline: String = "Set up your cycle",
    val cycleSub: String = "Add your last period date to get personalised insights.",
    val todayFocus: String? = null,
    val hydrationLitres: Float? = null,
    val hydrationGoalLitres: Float = 2.4f,
    val streakDays: Int? = null,
    val isLoading: Boolean = true,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
    private val cycleRepository: CycleRepository,
    private val dailyLogRepository: DailyLogRepository,
    private val prefs: OnboardingPreferences,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.currentUserId.collectLatest { userId ->
                if (userId == null) {
                    _uiState.value = HomeUiState(isLoading = false)
                    return@collectLatest
                }
                combine(
                    profileRepository.getProfile(userId),
                    cycleRepository.getCycleSettings(userId),
                    dailyLogRepository.getDailyLog(userId, LocalDate.now()),
                    prefs.userName,
                ) { profile, cycleSettings, todayLog, prefName ->
                    val name = profile?.displayName?.ifBlank { null } ?: prefName.ifBlank { "there" }
                    if (cycleSettings == null) {
                        return@combine HomeUiState(userName = name, cycleSetUp = false, isLoading = false)
                    }
                    val info = CycleEngine.getCyclePhase(
                        CycleSettings(cycleSettings.lastPeriodDate, cycleSettings.cycleLength, cycleSettings.periodLength)
                    )
                    val focusFood = phaseFoods[info.phase]?.firstOrNull()
                    HomeUiState(
                        userName = name,
                        cycleSetUp = true,
                        cycleEyebrow = "DAY ${info.dayOfCycle} · ${info.phase.name.lowercase().replaceFirstChar { it.uppercase() }}",
                        cycleHeadline = when (info.daysUntilNextPeriod) {
                            0 -> "Period expected today"
                            1 -> "Period expected tomorrow"
                            else -> "${info.daysUntilNextPeriod} days until next period"
                        },
                        cycleSub = "Cycle day ${info.dayOfCycle} of ${cycleSettings.cycleLength}",
                        todayFocus = focusFood?.let { "Focus: ${it.title}" },
                        hydrationLitres = todayLog?.waterMl?.div(1000f),
                        isLoading = false,
                    )
                }.collectLatest { state ->
                    _uiState.value = state
                    val streak = dailyLogRepository.getStreakDays(userId)
                    _uiState.value = _uiState.value.copy(streakDays = streak)
                }
            }
        }
    }
}
