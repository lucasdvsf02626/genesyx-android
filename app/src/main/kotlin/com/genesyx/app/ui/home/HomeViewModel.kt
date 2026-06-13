package com.genesyx.app.ui.home

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/** Home screen UI state. Mirrors the web Home (greeting, cycle hero, hydration, streak). */
data class HomeUiState(
    val userName: String = "Guest",
    val cycleSetUp: Boolean = false,
    val cycleEyebrow: String = "TODAY",
    val cycleHeadline: String = "Set up your cycle",
    val cycleSub: String = "Add your last period date to get personalised insights.",
    val cycleTags: List<String> = emptyList(),
    val todayFocus: String? = null,
    val hydrationLitres: Float? = null,
    val hydrationGoalLitres: Float = 2.4f,
    val streakDays: Int? = null,
    val isLoading: Boolean = false,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    // TODO(phase C): inject ProfileRepository, CycleRepository, DailyLogRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
}
