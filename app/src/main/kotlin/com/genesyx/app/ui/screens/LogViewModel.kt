package com.genesyx.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.genesyx.app.data.local.entity.DailyLogEntity
import com.genesyx.app.data.repository.AuthRepository
import com.genesyx.app.data.repository.DailyLogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

data class LogUiState(
    val mood: String? = null,
    val energy: String? = null,
    val symptoms: Set<String> = emptySet(),
    val waterMl: Int = 0,
    val sleepMinutes: Int = 480,
    val supplements: Set<String> = emptySet(),
    val notes: String = "",
    val isSaving: Boolean = false,
    val saved: Boolean = false,
)

@HiltViewModel
class LogViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val dailyLogRepository: DailyLogRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LogUiState())
    val uiState: StateFlow<LogUiState> = _uiState.asStateFlow()

    fun onMoodChange(v: String) { _uiState.value = _uiState.value.copy(mood = v) }
    fun onEnergyChange(v: String) { _uiState.value = _uiState.value.copy(energy = v) }
    fun toggleSymptom(v: String) {
        val set = _uiState.value.symptoms.toMutableSet()
        if (!set.add(v)) set.remove(v)
        _uiState.value = _uiState.value.copy(symptoms = set)
    }
    fun onWaterChange(delta: Int) {
        _uiState.value = _uiState.value.copy(waterMl = (_uiState.value.waterMl + delta).coerceAtLeast(0))
    }
    fun onSleepChange(delta: Int) {
        _uiState.value = _uiState.value.copy(sleepMinutes = (_uiState.value.sleepMinutes + delta).coerceIn(0, 1440))
    }
    fun toggleSupplement(v: String) {
        val set = _uiState.value.supplements.toMutableSet()
        if (!set.add(v)) set.remove(v)
        _uiState.value = _uiState.value.copy(supplements = set)
    }
    fun onNotesChange(v: String) { _uiState.value = _uiState.value.copy(notes = v) }

    fun save() {
        val s = _uiState.value
        _uiState.value = s.copy(isSaving = true)
        viewModelScope.launch {
            val userId = authRepository.currentUserId.first() ?: return@launch
            dailyLogRepository.saveLog(
                DailyLogEntity(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    date = LocalDate.now(),
                    mood = s.mood,
                    energy = s.energy,
                    symptoms = s.symptoms.toList(),
                    sleepMinutes = s.sleepMinutes,
                    waterMl = s.waterMl,
                    supplements = s.supplements.toList(),
                    notes = s.notes.ifBlank { null },
                )
            )
            _uiState.value = _uiState.value.copy(isSaving = false, saved = true)
        }
    }
}
