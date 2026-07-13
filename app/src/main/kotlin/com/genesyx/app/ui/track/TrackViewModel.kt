package com.genesyx.app.ui.track

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.genesyx.app.data.CycleRepository
import com.genesyx.app.data.DailyLogRepository
import com.genesyx.app.data.PhRepository
import com.genesyx.app.domain.model.CycleSettings
import com.genesyx.app.domain.model.LogDay
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class TrackViewModel @Inject constructor(
    private val cycleRepository: CycleRepository,
    dailyLogRepository: DailyLogRepository,
    phRepository: PhRepository,
) : ViewModel() {

    val settings: StateFlow<CycleSettings?> = cycleRepository.settings

    /**
     * What the user actually logged, keyed by day, so tapping a calendar date can show it. The
     * calendar had no access to this at all and so told every past day "No log yet for this day",
     * including days with a full log.
     *
     * Same merge as the "My logs" timeline (the daily log plus that day's pH readings), keyed by
     * date rather than sorted, because the caller looks a day up rather than iterating.
     */
    val logDays: StateFlow<Map<LocalDate, LogDay>> = combine(
        dailyLogRepository.logByDate,
        phRepository.readings,
    ) { logs, readings ->
        val phByDay = readings.groupBy { it.recordedAt.toLocalDate() }
        (logs.keys + phByDay.keys).associateWith { date ->
            LogDay(
                date = date,
                dailyLog = logs[date],
                phReadings = phByDay[date].orEmpty().sortedBy { it.recordedAt },
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    fun saveCycleSettings(settings: CycleSettings) = cycleRepository.upsert(settings)
}
