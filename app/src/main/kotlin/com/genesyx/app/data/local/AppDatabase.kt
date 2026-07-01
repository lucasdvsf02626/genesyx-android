package com.genesyx.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.genesyx.app.data.local.dao.CycleSettingsDao
import com.genesyx.app.data.local.dao.DailyLogDao
import com.genesyx.app.data.local.dao.PartnerInviteDao
import com.genesyx.app.data.local.dao.PhReadingDao
import com.genesyx.app.data.local.dao.ProfileDao
import com.genesyx.app.data.local.entity.CycleSettingsEntity
import com.genesyx.app.data.local.entity.DailyLogEntity
import com.genesyx.app.data.local.entity.PartnerInviteEntity
import com.genesyx.app.data.local.entity.PhReadingEntity
import com.genesyx.app.data.local.entity.ProfileEntity

@Database(
    entities = [
        ProfileEntity::class,
        CycleSettingsEntity::class,
        DailyLogEntity::class,
        PhReadingEntity::class,
        PartnerInviteEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun cycleSettingsDao(): CycleSettingsDao
    abstract fun dailyLogDao(): DailyLogDao
    abstract fun phReadingDao(): PhReadingDao
    abstract fun partnerInviteDao(): PartnerInviteDao
}
