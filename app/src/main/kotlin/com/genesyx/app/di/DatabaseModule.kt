package com.genesyx.app.di

import android.content.Context
import androidx.room.Room
import com.genesyx.app.data.local.GENESYX_MIGRATIONS
import com.genesyx.app.data.local.GenesyxDatabase
import com.genesyx.app.data.local.dao.ClientDao
import com.genesyx.app.data.local.dao.CycleSettingsDao
import com.genesyx.app.data.local.dao.DailyLogDao
import com.genesyx.app.data.local.dao.PartnerDao
import com.genesyx.app.data.local.dao.PhReadingDao
import com.genesyx.app.data.local.dao.ProfileDao
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
    fun provideDatabase(@ApplicationContext context: Context): GenesyxDatabase =
        Room.databaseBuilder(context, GenesyxDatabase::class.java, "genesyx.db")
            // Real migrations only — NO destructive fallback, so schema bumps preserve local data
            // (critical for LOCAL-ONLY pH readings, which have no server copy in v1.0). Add each
            // MIGRATION_x_y to GENESYX_MIGRATIONS; a missing migration now fails loudly instead of
            // silently wiping the user's data.
            .addMigrations(*GENESYX_MIGRATIONS)
            // Narrow safety valve: only a DOWNGRADE (older app over newer schema) resets the DB.
            // Upgrade protection is unchanged — upgrades must have a real migration.
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()

    @Provides fun provideCycleSettingsDao(db: GenesyxDatabase): CycleSettingsDao = db.cycleSettingsDao()
    @Provides fun provideDailyLogDao(db: GenesyxDatabase): DailyLogDao = db.dailyLogDao()
    @Provides fun providePhReadingDao(db: GenesyxDatabase): PhReadingDao = db.phReadingDao()
    @Provides fun provideProfileDao(db: GenesyxDatabase): ProfileDao = db.profileDao()
    @Provides fun provideClientDao(db: GenesyxDatabase): ClientDao = db.clientDao()
    @Provides fun providePartnerDao(db: GenesyxDatabase): PartnerDao = db.partnerDao()
}
