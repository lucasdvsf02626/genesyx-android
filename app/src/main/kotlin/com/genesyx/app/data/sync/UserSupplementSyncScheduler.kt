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

/** Schedules a background retry of the user-supplement sync queue. Abstracted so
 *  UserSupplementRepository stays JVM-testable. */
interface UserSupplementSyncScheduler {
    fun schedule()

    /** User-initiated retry ("Sync now"): re-enqueue so a stuck drain's constraint re-evaluates. */
    fun syncNow()
}

@Singleton
class WorkManagerUserSupplementSyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) : UserSupplementSyncScheduler {
    override fun schedule() {
        // KEEP: if a drain is already queued, don't stack duplicates — it already covers all PENDING rows.
        enqueue(ExistingWorkPolicy.KEEP)
    }

    override fun syncNow() {
        // REPLACE: a request queued behind a mis-evaluated network constraint (captive portal,
        // failed validation) waits forever under KEEP. Replacing is safe — the drain is idempotent,
        // it just re-pushes whatever is still PENDING.
        enqueue(ExistingWorkPolicy.REPLACE)
    }

    private fun enqueue(policy: ExistingWorkPolicy) {
        val request = OneTimeWorkRequestBuilder<UserSupplementSyncWorker>()
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build(),
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(WORK_NAME, policy, request)
    }

    private companion object {
        const val WORK_NAME = "user-supplement-sync"
    }
}
