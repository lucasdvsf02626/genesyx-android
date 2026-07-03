package com.genesyx.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.genesyx.app.data.local.dao.ClientDao
import com.genesyx.app.data.local.dao.CycleSettingsDao
import com.genesyx.app.data.local.dao.DailyLogDao
import com.genesyx.app.data.local.dao.PartnerDao
import com.genesyx.app.data.local.dao.PhReadingDao
import com.genesyx.app.data.local.dao.ProfileDao
import com.genesyx.app.data.local.entity.ClientEntity
import com.genesyx.app.data.local.entity.CycleSettingsEntity
import com.genesyx.app.data.local.entity.DailyLogEntity
import com.genesyx.app.data.local.entity.PartnerInviteEntity
import com.genesyx.app.data.local.entity.PartnerLinkEntity
import com.genesyx.app.data.local.entity.PhReadingEntity
import com.genesyx.app.data.local.entity.ProfileEntity

/**
 * Offline-first local store mirroring the Supabase schema (docs/schema.sql). Every row is scoped by
 * userId/ownerUserId so multiple accounts stay isolated on-device and map 1:1 to Supabase RLS.
 */
@Database(
    entities = [
        CycleSettingsEntity::class,
        DailyLogEntity::class,
        PhReadingEntity::class,
        ProfileEntity::class,
        ClientEntity::class,
        PartnerInviteEntity::class,
        PartnerLinkEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class GenesyxDatabase : RoomDatabase() {
    abstract fun cycleSettingsDao(): CycleSettingsDao
    abstract fun dailyLogDao(): DailyLogDao
    abstract fun phReadingDao(): PhReadingDao
    abstract fun profileDao(): ProfileDao
    abstract fun clientDao(): ClientDao
    abstract fun partnerDao(): PartnerDao
}
