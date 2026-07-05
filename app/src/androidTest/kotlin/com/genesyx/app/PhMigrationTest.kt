package com.genesyx.app

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.genesyx.app.data.local.GenesyxDatabase
import com.genesyx.app.data.local.MIGRATION_2_3
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Proves MIGRATION_2_3 preserves existing (LOCAL-ONLY) pH rows instead of wiping them, and seeds the
 * new sync columns sensibly. Validates the migrated schema against the committed 3.json.
 */
@RunWith(AndroidJUnit4::class)
class PhMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        GenesyxDatabase::class.java,
    )

    @Test
    fun migrate2To3_preservesPhRows_andSeedsSyncColumns() {
        helper.createDatabase(DB, 2).apply {
            execSQL(
                "INSERT INTO ph_readings (id, userId, phValue, recordedAt, notes) " +
                    "VALUES ('r1', 'user-a', 6.5, '2026-01-01T09:00:00', NULL)",
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(DB, 3, true, MIGRATION_2_3)

        db.query("SELECT id, phValue, syncStatus, updatedAt, deletedAt FROM ph_readings WHERE id = 'r1'").use { c ->
            assertEquals(1, c.count)                       // row survived the migration
            c.moveToFirst()
            assertEquals("r1", c.getString(0))
            assertEquals(6.5, c.getDouble(1), 0.0001)
            assertEquals("SYNCED", c.getString(2))         // defaulted
            assertEquals("2026-01-01T09:00:00", c.getString(3)) // seeded from recordedAt
            assertEquals(null, c.getString(4))             // deletedAt null
        }
    }

    private companion object {
        const val DB = "migration-test"
    }
}
