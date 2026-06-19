package com.genesyx.app.ui.screens

import androidx.lifecycle.ViewModel
import com.genesyx.app.data.DailyLogRepository
import com.genesyx.app.domain.model.DailyLog
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class LogViewModel @Inject constructor(
    private val dailyLogRepository: DailyLogRepository,
) : ViewModel() {

    fun todaysLog(): DailyLog = dailyLogRepository.logOn(LocalDate.now())

    fun save(log: DailyLog) = dailyLogRepository.upsert(LocalDate.now(), log)
}
