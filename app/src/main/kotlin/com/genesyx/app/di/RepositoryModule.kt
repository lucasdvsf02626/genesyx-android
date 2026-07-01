package com.genesyx.app.di

import com.genesyx.app.data.repository.AuthRepository
import com.genesyx.app.data.repository.CycleRepository
import com.genesyx.app.data.repository.DailyLogRepository
import com.genesyx.app.data.repository.PartnerRepository
import com.genesyx.app.data.repository.PhRepository
import com.genesyx.app.data.repository.ProfileRepository
import com.genesyx.app.data.repository.SupabaseAuthRepository
import com.genesyx.app.data.repository.SupabaseCycleRepository
import com.genesyx.app.data.repository.SupabaseDailyLogRepository
import com.genesyx.app.data.repository.SupabasePartnerRepository
import com.genesyx.app.data.repository.SupabasePhRepository
import com.genesyx.app.data.repository.SupabaseProfileRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton abstract fun bindAuthRepository(impl: SupabaseAuthRepository): AuthRepository
    @Binds @Singleton abstract fun bindProfileRepository(impl: SupabaseProfileRepository): ProfileRepository
    @Binds @Singleton abstract fun bindCycleRepository(impl: SupabaseCycleRepository): CycleRepository
    @Binds @Singleton abstract fun bindDailyLogRepository(impl: SupabaseDailyLogRepository): DailyLogRepository
    @Binds @Singleton abstract fun bindPhRepository(impl: SupabasePhRepository): PhRepository
    @Binds @Singleton abstract fun bindPartnerRepository(impl: SupabasePartnerRepository): PartnerRepository
}
