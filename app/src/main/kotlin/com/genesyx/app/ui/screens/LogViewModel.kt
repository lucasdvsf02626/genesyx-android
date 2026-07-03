package com.genesyx.app.ui.screens

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.ViewModel
import com.genesyx.app.data.DailyLogRepository
import com.genesyx.app.domain.model.DailyLog
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class LogViewModel @Inject constructor(
    private val dailyLogRepository: DailyLogRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    fun todaysLog(): DailyLog = dailyLogRepository.logOn(LocalDate.now())

    fun save(log: DailyLog) = dailyLogRepository.upsert(LocalDate.now(), log)

    /**
     * Point-in-time connectivity check. v1.0 blocks a log save while offline because there is no
     * sync queue yet: an offline write lands in Room but is silently overwritten by the server on the
     * next read-through (data loss). A real offline-first queue is a v1.1 task.
     */
    fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
