package com.genesyx.app.di

import com.genesyx.app.BuildConfig
import com.genesyx.app.auth.AuthService
import com.genesyx.app.auth.LocalAuthService
import com.genesyx.app.auth.SupabaseAuthService
import com.genesyx.app.core.config.AppConfig
import com.genesyx.app.data.remote.CycleRemoteDataSource
import com.genesyx.app.data.remote.DailyLogRemoteDataSource
import com.genesyx.app.data.remote.PhRemoteDataSource
import com.genesyx.app.data.remote.ProfileRemoteDataSource
import com.genesyx.app.data.remote.StubCycleRemoteDataSource
import com.genesyx.app.data.remote.StubDailyLogRemoteDataSource
import com.genesyx.app.data.remote.StubPhRemoteDataSource
import com.genesyx.app.data.remote.SupabaseCycleRemoteDataSource
import com.genesyx.app.data.remote.SupabaseDailyLogRemoteDataSource
import com.genesyx.app.data.remote.SupabasePhRemoteDataSource
import com.genesyx.app.data.remote.GenesyxProductRemoteDataSource
import com.genesyx.app.data.remote.StubGenesyxProductRemoteDataSource
import com.genesyx.app.data.remote.StubProfileRemoteDataSource
import com.genesyx.app.data.remote.StubUserSupplementRemoteDataSource
import com.genesyx.app.data.remote.QuizAnswersRemoteDataSource
import com.genesyx.app.data.remote.StubQuizAnswersRemoteDataSource
import com.genesyx.app.data.remote.SupabaseQuizAnswersRemoteDataSource
import com.genesyx.app.data.remote.StubWaitlistRemoteDataSource
import com.genesyx.app.data.remote.SupabaseGenesyxProductRemoteDataSource
import com.genesyx.app.data.remote.SupabaseProfileRemoteDataSource
import com.genesyx.app.data.remote.SupabaseUserSupplementRemoteDataSource
import com.genesyx.app.data.remote.SupabaseWaitlistRemoteDataSource
import com.genesyx.app.data.remote.UserSupplementRemoteDataSource
import com.genesyx.app.data.remote.WaitlistRemoteDataSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import javax.inject.Provider
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds

/**
 * Provides the Supabase client (auth + Postgrest). Created lazily by Hilt the first time
 * a repository injects it. Configure credentials via gradle properties (see gradle.properties):
 *   genesyx.supabaseUrl, genesyx.supabaseAnonKey
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_ANON_KEY,
    ) {
        // 30s tolerates slow/mobile networks better than the 10s default (observed sign-in timeouts).
        requestTimeout = 30.seconds
        install(Auth)
        install(Postgrest)
    }

    /**
     * Local-first auth selection: use the real Supabase auth service when creds are configured,
     * otherwise fall back to the local service so the app stays usable. `Provider` ensures the
     * SupabaseClient is only constructed when actually selected (never with blank creds).
     */
    @Provides
    @Singleton
    fun provideAuthService(
        config: AppConfig,
        supabaseAuth: Provider<SupabaseAuthService>,
        localAuth: Provider<LocalAuthService>,
    ): AuthService = if (config.hasSupabase) supabaseAuth.get() else localAuth.get()

    /** Profile remote source: real Supabase when configured, else local-first stub. */
    @Provides
    @Singleton
    fun provideProfileRemoteDataSource(
        config: AppConfig,
        supabaseImpl: Provider<SupabaseProfileRemoteDataSource>,
        stub: Provider<StubProfileRemoteDataSource>,
    ): ProfileRemoteDataSource = if (config.hasSupabase) supabaseImpl.get() else stub.get()

    /** Cycle-settings remote source: real Supabase when configured, else local-first stub. */
    @Provides
    @Singleton
    fun provideCycleRemoteDataSource(
        config: AppConfig,
        supabaseImpl: Provider<SupabaseCycleRemoteDataSource>,
        stub: Provider<StubCycleRemoteDataSource>,
    ): CycleRemoteDataSource = if (config.hasSupabase) supabaseImpl.get() else stub.get()

    /** Daily-log remote source: real Supabase when configured, else local-first stub. */
    @Provides
    @Singleton
    fun provideDailyLogRemoteDataSource(
        config: AppConfig,
        supabaseImpl: Provider<SupabaseDailyLogRemoteDataSource>,
        stub: Provider<StubDailyLogRemoteDataSource>,
    ): DailyLogRemoteDataSource = if (config.hasSupabase) supabaseImpl.get() else stub.get()

    /** pH-readings remote source: real Supabase when configured, else local-first stub. */
    @Provides
    @Singleton
    fun providePhRemoteDataSource(
        config: AppConfig,
        supabaseImpl: Provider<SupabasePhRemoteDataSource>,
        stub: Provider<StubPhRemoteDataSource>,
    ): PhRemoteDataSource = if (config.hasSupabase) supabaseImpl.get() else stub.get()

    /** Waitlist remote source: real Supabase when configured, else local-first stub. */
    @Provides
    @Singleton
    fun provideWaitlistRemoteDataSource(
        config: AppConfig,
        supabaseImpl: Provider<SupabaseWaitlistRemoteDataSource>,
        stub: Provider<StubWaitlistRemoteDataSource>,
    ): WaitlistRemoteDataSource = if (config.hasSupabase) supabaseImpl.get() else stub.get()

    /** Quiz-answers remote source: real Supabase when configured, else local-first stub. */
    @Provides
    @Singleton
    fun provideQuizAnswersRemoteDataSource(
        config: AppConfig,
        supabaseImpl: Provider<SupabaseQuizAnswersRemoteDataSource>,
        stub: Provider<StubQuizAnswersRemoteDataSource>,
    ): QuizAnswersRemoteDataSource = if (config.hasSupabase) supabaseImpl.get() else stub.get()

    /** User-supplement remote source: real Supabase when configured, else local-first stub. */
    @Provides
    @Singleton
    fun provideUserSupplementRemoteDataSource(
        config: AppConfig,
        supabaseImpl: Provider<SupabaseUserSupplementRemoteDataSource>,
        stub: Provider<StubUserSupplementRemoteDataSource>,
    ): UserSupplementRemoteDataSource = if (config.hasSupabase) supabaseImpl.get() else stub.get()

    /** Genesyx product catalogue: real Supabase when configured, else local-first stub. */
    @Provides
    @Singleton
    fun provideGenesyxProductRemoteDataSource(
        config: AppConfig,
        supabaseImpl: Provider<SupabaseGenesyxProductRemoteDataSource>,
        stub: Provider<StubGenesyxProductRemoteDataSource>,
    ): GenesyxProductRemoteDataSource = if (config.hasSupabase) supabaseImpl.get() else stub.get()
}
