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
}

@Singleton
class WorkManagerDailyLogSyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) : DailyLogSyncScheduler {
    override fun schedule() {
        val request = OneTimeWorkRequestBuilder<DailyLogSyncWorker>()
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build(),
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        // KEEP: a queued drain already covers every PENDING row — don't stack duplicates.
        WorkManager.getInstance(context)
            .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.KEEP, request)
    }

    private companion object {
        const val WORK_NAME = "daily-log-sync"
    }
}
