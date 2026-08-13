package com.genesyx.app

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.genesyx.app.data.local.GenesyxDatabase
import com.genesyx.app.data.local.MIGRATION_7_8
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Proves MIGRATION_7_8 adds the local-only `meal_entries` table without disturbing existing data,
 * and that the DDL matches Room's generated schema (runMigrationsAndValidate(..., true) fails
 * loudly if it doesn't) — the check that a schema bump can't crash the app on upgrade.
 */
@RunWith(AndroidJUnit4::class)
class MealLogMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        GenesyxDatabase::class.java,
    )

    @Test
    fun migrate7To8_addsMealEntries_andPreservesExistingLogs() {
        helper.createDatabase(DB, 7).apply {
            execSQL(
                "INSERT INTO daily_logs (userId, date, moodId, energyId, symptoms, sleepMinutes, " +
                    "supplements, notes, waterMl, syncStatus) VALUES " +
                    "('user-a', 20000, 'good', 'low', '', 450, '', 'kept', 1500, 'SYNCED')",
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(DB, 8, true, MIGRATION_7_8)

        // The pre-existing daily log survived the upgrade.
        db.query("SELECT notes FROM daily_logs WHERE userId = 'user-a'").use { c ->
            assertEquals(1, c.count)
            c.moveToFirst()
            assertEquals("kept", c.getString(0))
        }
        // The new table exists and accepts a row (date is an epoch-day Long; nutrients a joined string).
        db.execSQL(
            "INSERT INTO meal_entries (id, userId, date, mealType, description, nutrients, loggedAt) " +
                "VALUES ('m1', 'user-a', 20000, 'lunch', 'Lentil salad', 'iron', '2026-08-12T13:30')",
        )
        db.query("SELECT description, mealType FROM meal_entries WHERE id = 'm1'").use { c ->
            assertEquals(1, c.count)
            c.moveToFirst()
            assertEquals("Lentil salad", c.getString(0))
            assertEquals("lunch", c.getString(1))
        }
    }

    private companion object {
        const val DB = "meal-log-migration-test"
    }
}
