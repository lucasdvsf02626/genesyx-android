package com.genesyx.app.data.local

import androidx.room.migration.Migration

/**
 * Ordered Room migrations for [GenesyxDatabase]. Every schema-version bump MUST add a
 * `MIGRATION_x_y` here — the database is built WITHOUT destructive fallback so upgrades preserve
 * local data instead of wiping it. This matters most for the LOCAL-ONLY pH readings, which have no
 * server copy in v1.0: a destructive migration would delete them permanently.
 *
 * Empty until the first bump. The v1.1 pH sync work (adds ph_readings.syncStatus / deleted_at)
 * lands MIGRATION_2_3 here.
 */
val GENESYX_MIGRATIONS: Array<Migration> = arrayOf(
    // MIGRATION_2_3 — added in Phase 3 (pH offline-first sync columns).
)
