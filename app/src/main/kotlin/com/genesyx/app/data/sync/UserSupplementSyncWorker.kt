package com.genesyx.app.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.genesyx.app.data.UserSupplementRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * Drains the user-supplement sync queue in the background with WorkManager backoff. Dependencies
 * are pulled via a Hilt EntryPoint (WorkManager instantiates the worker itself, so it isn't
 * Hilt-constructed). Returns retry() while anything is still PENDING.
 */
class UserSupplementSyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val repo = EntryPointAccessors
            .fromApplication(applicationContext, UserSupplementSyncEntryPoint::class.java)
            .userSupplementRepository()
        return if (repo.syncPending()) Result.success() else Result.retry()
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface UserSupplementSyncEntryPoint {
        fun userSupplementRepository(): UserSupplementRepository
    }
}
