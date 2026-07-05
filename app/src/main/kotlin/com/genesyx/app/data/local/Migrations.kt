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

val GENESYX_MIGRATIONS: Array<Migration> = arrayOf(
    MIGRATION_2_3,
)
