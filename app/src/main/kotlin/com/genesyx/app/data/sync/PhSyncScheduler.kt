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

/** Schedules a background retry of the pH sync queue. Abstracted so PhRepository stays JVM-testable. */
interface PhSyncScheduler {
    fun schedule()
}

@Singleton
class WorkManagerPhSyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) : PhSyncScheduler {
    override fun schedule() {
        val request = OneTimeWorkRequestBuilder<PhSyncWorker>()
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build(),
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        // KEEP: if a drain is already queued, don't stack duplicates — it already covers all PENDING rows.
        WorkManager.getInstance(context)
            .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.KEEP, request)
    }

    private companion object {
        const val WORK_NAME = "ph-sync"
    }
}
