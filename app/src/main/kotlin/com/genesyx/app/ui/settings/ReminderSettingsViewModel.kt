package com.genesyx.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.genesyx.app.data.NotificationSettingsRepository
import com.genesyx.app.notifications.ReminderScheduler
import com.genesyx.app.notifications.model.NotificationSettings
import com.genesyx.app.notifications.model.ReminderKind
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalTime
import javax.inject.Inject

/**
 * Drives the Reminders screen. Every write persists through [NotificationSettingsRepository] and then
 * re-arms the WorkManager chains — settings changes must reach the schedule immediately, or a user
 * who moves her 9pm reminder to 8pm still gets it at 9.
 */
@HiltViewModel
class ReminderSettingsViewModel @Inject constructor(
    private val repository: NotificationSettingsRepository,
    private val scheduler: ReminderScheduler,
) : ViewModel() {

    val settings: StateFlow<NotificationSettings> =
        repository.settings.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = NotificationSettings(),
        )

    fun setKindEnabled(kind: ReminderKind, enabled: Boolean) = viewModelScope.launch {
        repository.setKindEnabled(kind, enabled)
        if (enabled) repository.setMasterEnabled(true)
        applySchedule()
    }

    fun setDailyTime(time: LocalTime) = edit { repository.setDailyTime(time) }
    fun setDailyDays(days: Set<DayOfWeek>) = edit { repository.setDailyDays(days) }
    fun setWeeklyTime(time: LocalTime) = edit { repository.setWeeklyTime(time) }
    fun setWeeklyDay(day: DayOfWeek) = edit { repository.setWeeklyDay(day) }
    fun setHydrationTime(time: LocalTime) = edit { repository.setHydrationTime(time) }
    fun setQuietHours(enabled: Boolean, start: LocalTime, end: LocalTime) =
        edit { repository.setQuietHours(enabled, start, end) }

    /** The permission sheet was accepted — record it so a later "denied twice" is distinguishable. */
    fun onPrompted() = viewModelScope.launch {
        repository.setPrompted(System.currentTimeMillis())
    }

    /** Permission just granted — stamp the grant time (for the 24h grace) and arm the schedule. */
    fun onPermissionGranted() = viewModelScope.launch {
        repository.recordFirstGrant(System.currentTimeMillis())
        repository.setMasterEnabled(true)
        applySchedule()
    }

    private fun edit(block: suspend () -> Unit) = viewModelScope.launch {
        block()
        applySchedule()
    }

    private suspend fun applySchedule() {
        val current = repository.current()
        if (current.masterEnabled) scheduler.rescheduleAll(current) else scheduler.cancelAll()
    }
}
