package com.genesyx.app.di

import android.content.Context
import androidx.room.Room
import com.genesyx.app.data.local.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "genesyx.db").build()

    @Provides fun provideProfileDao(db: AppDatabase) = db.profileDao()
    @Provides fun provideCycleSettingsDao(db: AppDatabase) = db.cycleSettingsDao()
    @Provides fun provideDailyLogDao(db: AppDatabase) = db.dailyLogDao()
    @Provides fun providePhReadingDao(db: AppDatabase) = db.phReadingDao()
    @Provides fun providePartnerInviteDao(db: AppDatabase) = db.partnerInviteDao()
}
