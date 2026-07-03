package com.genesyx.app.core.di

import com.genesyx.app.BuildConfig
import com.genesyx.app.core.DefaultDispatcherProvider
import com.genesyx.app.core.DispatcherProvider
import com.genesyx.app.core.config.AppConfig
import com.genesyx.app.core.config.Environment
import com.genesyx.app.core.log.AndroidLogger
import com.genesyx.app.core.log.Logger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

/** Wiring for the cross-cutting core layer: config, dispatchers, logging, app-scoped coroutines. */
@Module
@InstallIn(SingletonComponent::class)
object CoreModule {

    @Provides
    @Singleton
    fun provideAppConfig(): AppConfig = AppConfig(
        environment = runCatching { Environment.valueOf(BuildConfig.GENESYX_ENV) }
            .getOrDefault(Environment.DEV),
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseAnonKey = BuildConfig.SUPABASE_ANON_KEY,
        apiBaseUrl = BuildConfig.GENESYX_API_BASE_URL,
        googleWebClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID,
    )

    @Provides
    @Singleton
    fun provideDispatcherProvider(): DispatcherProvider = DefaultDispatcherProvider()

    @Provides
    @Singleton
    fun provideLogger(config: AppConfig): Logger = AndroidLogger(config)

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(dispatchers: DispatcherProvider): CoroutineScope =
        CoroutineScope(SupervisorJob() + dispatchers.default)
}
