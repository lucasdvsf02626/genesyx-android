package com.genesyx.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.genesyx.app.data.DailyLogRepository
import com.genesyx.app.domain.model.DailyLog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class LogViewModel @Inject constructor(
    private val dailyLogRepository: DailyLogRepository,
) : ViewModel() {

    /** The form must not seed itself until this is true — see [DailyLogRepository.loaded]. */
    val loaded: StateFlow<Boolean> = dailyLogRepository.loaded

    fun todaysLog(): DailyLog = dailyLogRepository.logOn(LocalDate.now())

    /**
     * Live hydration total for the Water mini-card. The form does NOT own this number — the quick-add
     * trackers do — so it renders the shared aggregate rather than a snapshot taken at open time.
     */
    val todayWaterMl: StateFlow<Int> = dailyLogRepository.logByDate
        .map { it[LocalDate.now()]?.waterMl ?: 0 }
        .stateIn(viewModelScope, SharingStarted.Eagerly, dailyLogRepository.waterMlOn(LocalDate.now()))

    /** Writes through the same repository path as the quick-add trackers. */
    fun setWater(ml: Int) = dailyLogRepository.setWater(ml)

    /**
     * Saves online or off. The repository writes to Room and queues the push if it fails, so there is
     * nothing to gain by checking connectivity first — v1.0's `isOnline()` gate existed only because
     * an offline write could be silently overwritten by the next read-through.
     *
     * Water is deliberately preserved from the stored row, not taken from [log] — see
     * [DailyLogRepository.upsertPreservingWater].
     */
    fun save(log: DailyLog) = dailyLogRepository.upsertPreservingWater(LocalDate.now(), log)
}
