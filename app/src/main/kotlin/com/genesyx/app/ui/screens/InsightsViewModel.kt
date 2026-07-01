package com.genesyx.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.genesyx.app.data.repository.AuthRepository
import com.genesyx.app.data.repository.DailyLogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class InsightsUiState(
    val streakDays: Int? = null,
    val logsThisMonth: Int? = null,
    val topSymptoms: List<String> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class InsightsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val dailyLogRepository: DailyLogRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(InsightsUiState())
    val uiState: StateFlow<InsightsUiState> = _uiState.asStateFlow()

    private var currentUserId: String? = null

    init {
        viewModelScope.launch {
            authRepository.currentUserId.collectLatest { uid ->
                currentUserId = uid
                if (uid == null) { _uiState.value = InsightsUiState(); return@collectLatest }
                dailyLogRepository.getRecentLogs(uid, 30).collectLatest { logs ->
                    val startOfMonth = LocalDate.now().withDayOfMonth(1)
                    val thisMonth = logs.count { it.date >= startOfMonth }
                    val topSymptoms = logs
                        .flatMap { it.symptoms }
                        .groupingBy { it }
                        .eachCount()
                        .entries
                        .sortedByDescending { it.value }
                        .take(3)
                        .map { it.key }
                    val streak = dailyLogRepository.getStreakDays(uid)
                    _uiState.value = InsightsUiState(
                        streakDays = streak,
                        logsThisMonth = thisMonth,
                        topSymptoms = topSymptoms,
                    )
                }
            }
        }
    }
}
