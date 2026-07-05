package com.genesyx.app.di

import com.genesyx.app.core.cloud.CloudApi
import com.genesyx.app.core.cloud.DefaultCloudApi
import com.genesyx.app.core.log.Analytics
import com.genesyx.app.core.log.NoopAnalytics
import com.genesyx.app.data.remote.ClientRemoteDataSource
import com.genesyx.app.data.remote.StubClientRemoteDataSource
import com.genesyx.app.data.sync.PhSyncScheduler
import com.genesyx.app.data.sync.WorkManagerPhSyncScheduler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Interface → implementation bindings. Swapping any local-first stub for a Supabase/Google Cloud impl
 * is a one-line change here — nothing else in the app references the concrete classes.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class BindingsModule {

    @Binds @Singleton
    abstract fun bindCloudApi(impl: DefaultCloudApi): CloudApi

    @Binds @Singleton
    abstract fun bindAnalytics(impl: NoopAnalytics): Analytics

    @Binds @Singleton
    abstract fun bindClientRemote(impl: StubClientRemoteDataSource): ClientRemoteDataSource

    @Binds @Singleton
    abstract fun bindPhSyncScheduler(impl: WorkManagerPhSyncScheduler): PhSyncScheduler
}
