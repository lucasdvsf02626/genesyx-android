package com.genesyx.app.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.genesyx.app.data.PhRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * Drains the pH sync queue in the background with WorkManager backoff. Dependencies are pulled via a
 * Hilt EntryPoint (WorkManager instantiates the worker itself, so it isn't Hilt-constructed).
 * Returns retry() while anything is still PENDING so WorkManager reschedules with backoff.
 */
class PhSyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val repo = EntryPointAccessors
            .fromApplication(applicationContext, PhSyncEntryPoint::class.java)
            .phRepository()
        return if (repo.syncPending()) Result.success() else Result.retry()
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface PhSyncEntryPoint {
        fun phRepository(): PhRepository
    }
}
