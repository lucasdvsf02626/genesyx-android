package com.genesyx.app.notifications

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.genesyx.app.notifications.model.NotificationSettings
import com.genesyx.app.notifications.model.ReminderKind
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

/** Schedules reminder deliveries. Interface so callers stay JVM-testable — mirrors PhSyncScheduler. */
interface ReminderScheduler {
    /** Re-arm every enabled schedulable kind; cancel the ones now off. Called on any settings write. */
    fun rescheduleAll(settings: NotificationSettings)

    /** Enqueue the single next occurrence of [kind] — the self-rescheduling step the worker calls. */
    fun scheduleNext(kind: ReminderKind, settings: NotificationSettings)

    fun cancel(kind: ReminderKind)
    fun cancelAll()
}

@Singleton
class WorkManagerReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) : ReminderScheduler {

    override fun rescheduleAll(settings: NotificationSettings) {
        SCHEDULABLE.forEach { kind ->
            if (settings.isEnabled(kind)) scheduleNext(kind, settings) else cancel(kind)
        }
    }

    override fun scheduleNext(kind: ReminderKind, settings: NotificationSettings) {
        if (kind !in SCHEDULABLE) return
        val now = ZonedDateTime.now()
        val next = ReminderPolicy.nextOccurrence(kind, settings, now)
        // At least a second, so a computed "now" never enqueues a zero/negative delay.
        val delay = Duration.between(now, next).coerceAtLeast(Duration.ofSeconds(1))

        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(ReminderWorker.KEY_KIND to kind.name))
            // No network constraint: a local reminder must fire in airplane mode (unlike the sync queue).
            .build()

        // REPLACE, not KEEP: settings changed, the old schedule is now wrong — throw it away.
        WorkManager.getInstance(context)
            .enqueueUniqueWork(kind.workName, ExistingWorkPolicy.REPLACE, request)
    }

    override fun cancel(kind: ReminderKind) {
        WorkManager.getInstance(context).cancelUniqueWork(kind.workName)
    }

    override fun cancelAll() {
        ReminderKind.entries.forEach { cancel(it) }
    }

    private companion object {
        /** Kinds that run on a clock chain. NUTRITION (phase-transition) is reserved for later. */
        val SCHEDULABLE = listOf(
            ReminderKind.DAILY_LOG,
            ReminderKind.MISSED_LOG,
            ReminderKind.HYDRATION,
            ReminderKind.WEEKLY_INSIGHTS,
            ReminderKind.REENGAGEMENT,
        )
    }
}
