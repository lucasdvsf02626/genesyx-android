package com.genesyx.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.genesyx.app.data.repository.AuthRepository
import com.genesyx.app.data.repository.CycleRepository
import com.genesyx.app.domain.cycle.CycleEngine
import com.genesyx.app.domain.model.CalendarCell
import com.genesyx.app.domain.model.CycleSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject

data class TrackUiState(
    val month: YearMonth = YearMonth.now(),
    val cells: List<CalendarCell> = emptyList(),
    val cycleSetUp: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TrackViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val cycleRepository: CycleRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrackUiState())
    val uiState: StateFlow<TrackUiState> = _uiState.asStateFlow()

    private val _month = MutableStateFlow(YearMonth.now())

    init {
        viewModelScope.launch {
            combine(
                authRepository.currentUserId.flatMapLatest { uid ->
                    if (uid == null) flowOf(null) else cycleRepository.getCycleSettings(uid)
                },
                _month,
            ) { settings, month ->
                if (settings == null) {
                    TrackUiState(month = month, cycleSetUp = false)
                } else {
                    val cs = CycleSettings(settings.lastPeriodDate, settings.cycleLength, settings.periodLength)
                    TrackUiState(month = month, cells = CycleEngine.buildMonthGrid(month, cs), cycleSetUp = true)
                }
            }.collectLatest { _uiState.value = it }
        }
    }

    fun prevMonth() { _month.value = _month.value.minusMonths(1) }
    fun nextMonth() { _month.value = _month.value.plusMonths(1) }
}
