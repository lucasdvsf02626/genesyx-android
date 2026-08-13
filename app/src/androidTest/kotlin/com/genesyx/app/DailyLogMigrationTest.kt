package com.genesyx.app

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.genesyx.app.data.local.GenesyxDatabase
import com.genesyx.app.data.local.MIGRATION_3_4
import com.genesyx.app.data.local.MIGRATION_6_7
import com.genesyx.app.data.local.MIGRATION_8_9
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Proves each `daily_logs` migration keeps existing logs instead of wiping them — the database has no
 * destructive fallback on upgrade, and a user's whole tracking history lives in this table — and that
 * each new column starts out honestly blank rather than asserting something about days that predate
 * it. Rows predating the sync queue default to SYNCED, which is the truth for them: they could only
 * have been written while the app still refused offline saves.
 *
 * Add a case here for every future `daily_logs` migration. Unit tests cannot catch this class of
 * defect: Room only validates the DDL against the exported schema when it actually opens a database.
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

    @Test
    fun migrate8To9_preservesLogs_andLeavesFoodGroupsUnrecorded() {
        helper.createDatabase(DB, 8).apply {
            execSQL(
                "INSERT INTO daily_logs (userId, date, moodId, energyId, symptoms, sleepMinutes, " +
                    "supplements, notes, waterMl, syncStatus, sexualActivity) VALUES " +
                    "('user-a', 20000, 'good', 'low', '', 450, '', 'kept', 1500, 'SYNCED', 1)",
            )
            close()
        }

        // `validateDroppedTables = true` also makes Room compare the migrated table against 9.json,
        // column defaults included — which is why the ALTER adds no DEFAULT clause.
        val db = helper.runMigrationsAndValidate(DB, 9, true, MIGRATION_8_9)

        db.query("SELECT userId, notes, sexualActivity, foodGroups FROM daily_logs WHERE userId = 'user-a'").use { c ->
            assertEquals(1, c.count) // the row survived
            c.moveToFirst()
            assertEquals("user-a", c.getString(0))
            assertEquals("kept", c.getString(1))
            assertEquals(1, c.getInt(2)) // the v7 column is untouched by the v9 ALTER
            // NULL, which Converters.toStringList reads back as an empty list — the truth for a day
            // logged before meals could be recorded at all.
            assertTrue(c.isNull(3))
        }
    }

    private companion object {
        const val DB = "daily-log-migration-test"
    }
}
