package com.genesyx.app.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schedules a background retry of the daily-log sync queue. Abstracted so DailyLogRepository stays
 * JVM-testable (the fake just records that a retry was asked for).
 */
interface DailyLogSyncScheduler {
    fun schedule()

    /** User-initiated retry ("Sync now"): re-enqueue so a stuck drain's constraint re-evaluates. */
    fun syncNow()

    /** Sign-out / account deletion: a queued drain must not wake against a dead session. */
    fun cancel()
}

@Singleton
class WorkManagerDailyLogSyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) : DailyLogSyncScheduler {
    override fun schedule() {
        // KEEP: a queued drain already covers every PENDING row — don't stack duplicates.
        enqueue(ExistingWorkPolicy.KEEP)
    }

    override fun syncNow() {
        // REPLACE: a request queued behind a mis-evaluated network constraint (captive portal,
        // failed validation) waits forever under KEEP. Replacing is safe — the drain is idempotent,
        // it just re-pushes whatever is still PENDING.
        enqueue(ExistingWorkPolicy.REPLACE)
    }

    override fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    private fun enqueue(policy: ExistingWorkPolicy) {
        val request = OneTimeWorkRequestBuilder<DailyLogSyncWorker>()
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build(),
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(WORK_NAME, policy, request)
    }

    private companion object {
        const val WORK_NAME = "daily-log-sync"
    }
}
