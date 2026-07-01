package com.genesyx.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.genesyx.app.data.repository.AuthRepository
import com.genesyx.app.data.repository.CycleRepository
import com.genesyx.app.domain.cycle.CycleEngine
import com.genesyx.app.domain.model.CycleSettings
import com.genesyx.app.domain.model.Phase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NutritionUiState(val phase: Phase? = null)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class NutritionViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val cycleRepository: CycleRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NutritionUiState())
    val uiState: StateFlow<NutritionUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.currentUserId.flatMapLatest { uid ->
                if (uid == null) flowOf(null) else cycleRepository.getCycleSettings(uid)
            }.collectLatest { s ->
                if (s == null) { _uiState.value = NutritionUiState(); return@collectLatest }
                val phase = CycleEngine.getCyclePhase(CycleSettings(s.lastPeriodDate, s.cycleLength, s.periodLength)).phase
                _uiState.value = NutritionUiState(phase = phase)
            }
        }
    }
}
