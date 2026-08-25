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

/** v5 -> v6: the `user_supplements` table (manual supplement entries), mirroring the Supabase
 *  table with the same offline-sync bookkeeping as ph_readings. DDL must match Room's expected
 *  schema for [com.genesyx.app.data.local.entity.UserSupplementEntity] exactly (see
 *  app/schemas/…/6.json after building) or Room rejects the database at open. */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `user_supplements` (" +
                "`id` TEXT NOT NULL, " +
                "`userId` TEXT NOT NULL, " +
                "`name` TEXT NOT NULL, " +
                "`dose` TEXT, " +
                "`timeOfDay` TEXT, " +
                "`productId` TEXT, " +
                "`createdAt` TEXT NOT NULL, " +
                "`syncStatus` TEXT NOT NULL DEFAULT 'SYNCED', " +
                "`updatedAt` TEXT, " +
                "`deletedAt` TEXT, " +
                "PRIMARY KEY(`id`))",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_user_supplements_userId_createdAt` " +
                "ON `user_supplements` (`userId`, `createdAt`)",
        )
    }
}

/** v6 -> v7: private intimacy column on daily logs. Nullable — NULL is "not recorded", which is
 *  what every existing row truthfully is. Room stores Boolean? as INTEGER. */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE daily_logs ADD COLUMN sexualActivity INTEGER")
    }
}

/** v7 -> v8: the `meal_entries` table (local-only meal log — no Supabase mirror, so no sync
 *  columns). DDL must match Room's expected schema for
 *  [com.genesyx.app.data.local.entity.MealEntryEntity] exactly (see app/schemas/…/8.json after
 *  building) or Room rejects the database at open. `date` is an epoch-day INTEGER (see Converters). */
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `meal_entries` (" +
                "`id` TEXT NOT NULL, " +
                "`userId` TEXT NOT NULL, " +
                "`date` INTEGER NOT NULL, " +
                "`mealType` TEXT NOT NULL, " +
                "`description` TEXT NOT NULL, " +
                "`nutrients` TEXT NOT NULL, " +
                "`loggedAt` TEXT NOT NULL, " +
                "PRIMARY KEY(`id`))",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_meal_entries_userId_date` " +
                "ON `meal_entries` (`userId`, `date`)",
        )
    }
}

/** v8 -> v9: food groups on daily logs, the local half of the shared `daily_logs.food_groups`
 *  column. Nullable and with no DEFAULT, matching the [MIGRATION_6_7] shape: Room compares column
 *  defaults when it validates a migration, and [Converters] already reads NULL back as an empty
 *  list — which is exactly what every pre-v9 row is. */
val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE daily_logs ADD COLUMN foodGroups TEXT")
    }
}

/** v9 -> v10: the health-data consent trail. Append-only, so there is no state column to update and
 *  no uniqueness constraint on the user — a row per grant and per withdrawal is the point.
 *  `recordedAt` is a TEXT timestamp (see [Converters]), matching `meal_entries.loggedAt`. Existing
 *  installs get an empty table, which reads as "no answer" and leaves the gate permissive: an
 *  upgrade must not silently stop tracking for someone who never had a consent screen to answer. */
val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `consent_events` (" +
                "`id` TEXT NOT NULL, " +
                "`userId` TEXT NOT NULL, " +
                "`action` TEXT NOT NULL, " +
                "`recordedAt` TEXT NOT NULL, " +
                "PRIMARY KEY(`id`))",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_consent_events_userId_recordedAt` " +
                "ON `consent_events` (`userId`, `recordedAt`)",
        )
    }
}

val GENESYX_MIGRATIONS: Array<Migration> = arrayOf(
    MIGRATION_2_3,
    MIGRATION_3_4,
    MIGRATION_4_5,
    MIGRATION_5_6,
    MIGRATION_6_7,
    MIGRATION_7_8,
    MIGRATION_8_9,
    MIGRATION_9_10,
)
