package com.genesyx.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.genesyx.app.data.DailyLogRepository
import com.genesyx.app.data.PhRepository
import com.genesyx.app.domain.model.LogDay
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Merges the daily-log and pH history (both already reactive, per-user Room flows) into a single
 * newest-first timeline for the "My logs" screen. Read-only; no new persistence or sync.
 */
@HiltViewModel
class LogHistoryViewModel @Inject constructor(
    phRepository: PhRepository,
    dailyLogRepository: DailyLogRepository,
) : ViewModel() {

    val days: StateFlow<List<LogDay>> = combine(
        phRepository.readings,
        dailyLogRepository.logByDate,
    ) { readings, logs ->
        val phByDay = readings.groupBy { it.recordedAt.toLocalDate() }
        (logs.keys + phByDay.keys).distinct().sortedDescending()
            .map { date ->
                LogDay(
                    date = date,
                    dailyLog = logs[date],
                    phReadings = phByDay[date].orEmpty().sortedBy { it.recordedAt },
                )
            }
            .filterNot { it.isEmpty }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
