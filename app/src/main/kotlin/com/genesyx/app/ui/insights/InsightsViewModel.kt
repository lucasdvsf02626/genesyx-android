package com.genesyx.app.ui.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.genesyx.app.data.PhRepository
import com.genesyx.app.domain.ph.PhStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

enum class Trend { UP, DOWN, FLAT }

data class PhInsights(
    val hasReadings: Boolean = false,
    val currentValue: Double? = null,
    val currentStatus: PhStatus? = null,
    val trend: Trend = Trend.FLAT,
    val avg7: Double? = null,
    val avg30: Double? = null,
    val insight: String = "Log a few pH readings to see personalised insights here.",
    val recommendation: String = "",
)

@HiltViewModel
class InsightsViewModel @Inject constructor(
    phRepository: PhRepository,
) : ViewModel() {

    val phInsights: StateFlow<PhInsights> =
        phRepository.readings
            .map { readings -> PhInsightLogic.compute(readings) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = PhInsights(),
            )
}
