package com.genesyx.app

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.genesyx.app.data.local.GenesyxDatabase
import com.genesyx.app.data.local.MIGRATION_2_3
import com.genesyx.app.data.local.MIGRATION_4_5
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Proves the pH migrations preserve existing rows instead of wiping them: MIGRATION_2_3 seeds the
 * sync columns, and MIGRATION_4_5 stamps existing rows as legacy 'urine'. Validates each migrated
 * schema against the committed schema JSON.
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

    @Test
    fun migrate4To5_preservesPhRows_andStampsThemUrine() {
        helper.createDatabase(DB, 4).apply {
            execSQL(
                "INSERT INTO ph_readings (id, userId, phValue, recordedAt) " +
                    "VALUES ('r1', 'user-a', 6.5, '2026-01-01T09:00:00')",
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(DB, 5, true, MIGRATION_4_5)

        db.query("SELECT id, phValue, measurementType FROM ph_readings WHERE id = 'r1'").use { c ->
            assertEquals(1, c.count)                       // row survived the migration
            c.moveToFirst()
            assertEquals("r1", c.getString(0))
            assertEquals(6.5, c.getDouble(1), 0.0001)      // value untouched
            assertEquals("urine", c.getString(2))          // existing rows stamped legacy urine
        }
    }

    private companion object {
        const val DB = "migration-test"
    }
}
