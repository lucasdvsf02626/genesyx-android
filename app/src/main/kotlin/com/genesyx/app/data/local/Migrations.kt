package com.genesyx.app.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Ordered Room migrations for [GenesyxDatabase]. Every schema-version bump MUST add a
 * `MIGRATION_x_y` here — the database is built WITHOUT destructive fallback (upgrades) so migrations
 * preserve local data instead of wiping it. This matters most for the LOCAL-ONLY pH readings.
 */

/** v2 -> v3: pH offline-first sync columns. Existing rows are treated as already SYNCED; their
 *  updatedAt is seeded from recordedAt so last-write-wins has a sane clock. */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE ph_readings ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'SYNCED'")
        db.execSQL("ALTER TABLE ph_readings ADD COLUMN updatedAt TEXT")
        db.execSQL("ALTER TABLE ph_readings ADD COLUMN deletedAt TEXT")
        db.execSQL("UPDATE ph_readings SET updatedAt = recordedAt WHERE updatedAt IS NULL")
    }
}

/** v3 -> v4: daily-log offline-sync column. Existing rows came from (or were pushed to) the server
 *  while the app blocked offline saves, so they are all SYNCED — the default is the truth here. */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE daily_logs ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'SYNCED'")
    }
}

/** v4 -> v5: pH measurement type. Every existing row predates the "Vaginal pH" switch, so it is a
 *  urine reading — the 'urine' default stamps them all, keeping legacy values distinguishable from
 *  vaginal readings (which are on a different scale). New writes use 'vaginal'. */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE ph_readings ADD COLUMN measurementType TEXT NOT NULL DEFAULT 'urine'")
    }
}

val GENESYX_MIGRATIONS: Array<Migration> = arrayOf(
    MIGRATION_2_3,
    MIGRATION_3_4,
    MIGRATION_4_5,
)
