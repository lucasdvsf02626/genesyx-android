package com.genesyx.app.ui.screens

import androidx.lifecycle.ViewModel
import com.genesyx.app.data.DailyLogRepository
import com.genesyx.app.domain.model.DailyLog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
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
     * Saves online or off. The repository writes to Room and queues the push if it fails, so there is
     * nothing to gain by checking connectivity first — v1.0's `isOnline()` gate existed only because
     * an offline write could be silently overwritten by the next read-through.
     */
    fun save(log: DailyLog) = dailyLogRepository.upsert(LocalDate.now(), log)
}
