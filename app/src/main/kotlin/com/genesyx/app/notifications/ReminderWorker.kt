package com.genesyx.app.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.genesyx.app.data.NotificationSettingsRepository
import com.genesyx.app.notifications.model.ReminderKind
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * Fires one reminder occurrence, then re-arms the chain. Dependencies come through a Hilt EntryPoint
 * because WorkManager constructs the worker itself (mirrors PhSyncWorker). It always succeeds — a
 * suppressed reminder is not a failure, and the next link is enqueued regardless so the chain never
 * dies on a quiet night.
 */
class ReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val kind = inputData.getString(KEY_KIND)
            ?.let { runCatching { ReminderKind.valueOf(it) }.getOrNull() }
            ?: return Result.success()

        val entry = EntryPointAccessors.fromApplication(applicationContext, ReminderEntryPoint::class.java)
        entry.notifier().postIfAllowed(kind)
        // Re-arm from the current clock, so tomorrow's occurrence (or the next valid day) is queued.
        entry.scheduler().scheduleNext(kind, entry.settingsRepository().current())
        return Result.success()
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ReminderEntryPoint {
        fun notifier(): ReminderNotifier
        fun scheduler(): ReminderScheduler
        fun settingsRepository(): NotificationSettingsRepository
    }

    companion object {
        const val KEY_KIND = "kind"
    }
}
