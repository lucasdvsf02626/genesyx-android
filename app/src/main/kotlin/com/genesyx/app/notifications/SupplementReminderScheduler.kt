package com.genesyx.app.notifications

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schedules the customer's own per-supplement daily reminders. Deliberately separate from the
 * intelligent-notification engine ([ReminderScheduler]/[ReminderPolicy]): these are explicit alarms
 * the customer set, so they don't count against the planner's daily cap or its suppression rules.
 *
 * One self-rescheduling WorkManager chain per supplement id, tagged [TAG] so the whole set can be
 * cancelled on sign-out. WorkManager is deferrable delivery, not an exact alarm — the copy never
 * promises to-the-minute timing.
 */
@Singleton
class SupplementReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /** Enqueue the next occurrence of one supplement's reminder (REPLACE — a changed time wins). */
    fun schedule(id: String, name: String, minutesOfDay: Int) {
        val now = ZonedDateTime.now()
        val delay = Duration.between(now, nextOccurrence(minutesOfDay, now))
            .coerceAtLeast(Duration.ofSeconds(1))
        val request = OneTimeWorkRequestBuilder<SupplementReminderWorker>()
            .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
            .setInputData(
                workDataOf(
                    SupplementReminderWorker.KEY_ID to id,
                    SupplementReminderWorker.KEY_NAME to name,
                    SupplementReminderWorker.KEY_MINUTES to minutesOfDay,
                ),
            )
            .addTag(TAG) // no network constraint: a local reminder must fire in airplane mode
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(workName(id), ExistingWorkPolicy.REPLACE, request)
    }

    fun cancel(id: String) {
        WorkManager.getInstance(context).cancelUniqueWork(workName(id))
    }

    /** Cancel every supplement reminder (sign-out / account deletion). */
    fun cancelAll() {
        WorkManager.getInstance(context).cancelAllWorkByTag(TAG)
    }

    companion object {
        const val TAG = "supplement-reminder"
        fun workName(id: String) = "supp-reminder-$id"

        /** The next time at [minutesOfDay] strictly after [now] — today if it's still ahead, else tomorrow. */
        fun nextOccurrence(minutesOfDay: Int, now: ZonedDateTime): ZonedDateTime {
            val time = LocalTime.of((minutesOfDay / 60) % 24, minutesOfDay % 60)
            var candidate = now.toLocalDate().atTime(time).atZone(now.zone)
            if (!candidate.isAfter(now)) candidate = candidate.plusDays(1)
            return candidate
        }
    }
}
