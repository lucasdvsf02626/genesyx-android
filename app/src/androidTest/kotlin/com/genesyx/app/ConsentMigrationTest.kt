package com.genesyx.app

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.genesyx.app.data.local.GenesyxDatabase
import com.genesyx.app.data.local.MIGRATION_9_10
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Proves MIGRATION_9_10 adds the `consent_events` trail without disturbing existing data, and that
 * the DDL matches Room's generated schema (runMigrationsAndValidate(..., true) fails loudly if it
 * doesn't) — the check that a schema bump can't crash the app on upgrade.
 *
 * The empty-table assertion is the compliance-relevant one: an upgrading install has never been
 * shown a consent screen, so the trail must come up empty and leave the gate permissive rather than
 * silently switching her tracking off.
 */
@RunWith(AndroidJUnit4::class)
class ConsentMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        GenesyxDatabase::class.java,
    )

    @Test
    fun migrate9To10_addsConsentEvents_andPreservesExistingLogs() {
        helper.createDatabase(DB, 9).apply {
            execSQL(
                "INSERT INTO daily_logs (userId, date, moodId, energyId, symptoms, sleepMinutes, " +
                    "supplements, notes, waterMl, syncStatus) VALUES " +
                    "('user-a', 20000, 'good', 'low', '', 450, '', 'kept', 1500, 'SYNCED')",
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(DB, 10, true, MIGRATION_9_10)

        // The pre-existing daily log survived the upgrade.
        db.query("SELECT notes FROM daily_logs WHERE userId = 'user-a'").use { c ->
            assertEquals(1, c.count)
            c.moveToFirst()
            assertEquals("kept", c.getString(0))
        }
        // No answer carried over — the gate reads an empty trail as permitted.
        db.query("SELECT id FROM consent_events").use { c -> assertEquals(0, c.count) }

        // The trail is append-only: two rows for the same user coexist, newest wins.
        db.execSQL(
            "INSERT INTO consent_events (id, userId, action, recordedAt) VALUES " +
                "('c1', 'user-a', 'granted', '2026-08-20T09:00'), " +
                "('c2', 'user-a', 'withdrawn', '2026-08-21T09:00')",
        )
        db.query(
            "SELECT action FROM consent_events WHERE userId = 'user-a' ORDER BY recordedAt DESC",
        ).use { c ->
            assertEquals(2, c.count)
            c.moveToFirst()
            assertEquals("withdrawn", c.getString(0))
        }
    }

    private companion object {
        const val DB = "consent-migration-test"
    }
}
