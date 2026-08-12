package com.genesyx.app

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.genesyx.app.data.local.GenesyxDatabase
import com.genesyx.app.data.local.MIGRATION_3_4
import com.genesyx.app.data.local.MIGRATION_6_7
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Proves MIGRATION_3_4 keeps existing daily logs instead of wiping them (the database has no
 * destructive fallback on upgrade, and a user's whole tracking history lives in this table), and that
 * rows predating the sync queue default to SYNCED — which is the truth for them: they could only have
 * been written while the app still refused offline saves.
 */
@RunWith(AndroidJUnit4::class)
class DailyLogMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        GenesyxDatabase::class.java,
    )

    @Test
    fun migrate3To4_preservesLogs_andDefaultsThemToSynced() {
        helper.createDatabase(DB, 3).apply {
            // date is stored as an epoch-day Long (see Converters) — 20_000 = 2024-10-04.
            execSQL(
                "INSERT INTO daily_logs (userId, date, moodId, energyId, symptoms, sleepMinutes, " +
                    "supplements, notes, waterMl) VALUES " +
                    "('user-a', 20000, 'good', 'low', '', 450, '', 'kept', 1500)",
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(DB, 4, true, MIGRATION_3_4)

        db.query("SELECT userId, waterMl, notes, syncStatus FROM daily_logs WHERE userId = 'user-a'").use { c ->
            assertEquals(1, c.count) // the row survived
            c.moveToFirst()
            assertEquals("user-a", c.getString(0))
            assertEquals(1500, c.getInt(1))
            assertEquals("kept", c.getString(2))
            assertEquals("SYNCED", c.getString(3)) // defaulted, not left null
        }
    }

    @Test
    fun migrate6To7_preservesLogs_andLeavesIntimacyUnrecorded() {
        helper.createDatabase(DB, 6).apply {
            execSQL(
                "INSERT INTO daily_logs (userId, date, moodId, energyId, symptoms, sleepMinutes, " +
                    "supplements, notes, waterMl, syncStatus) VALUES " +
                    "('user-a', 20000, 'good', 'low', '', 450, '', 'kept', 1500, 'SYNCED')",
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(DB, 7, true, MIGRATION_6_7)

        db.query("SELECT userId, notes, sexualActivity FROM daily_logs WHERE userId = 'user-a'").use { c ->
            assertEquals(1, c.count) // the row survived
            c.moveToFirst()
            assertEquals("user-a", c.getString(0))
            assertEquals("kept", c.getString(1))
            // NULL, not 0: pre-migration days are truthfully "not recorded", never "recorded no".
            assertTrue(c.isNull(2))
        }
    }

    private companion object {
        const val DB = "daily-log-migration-test"
    }
}
