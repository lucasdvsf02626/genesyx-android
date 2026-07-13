package com.genesyx.app.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.genesyx.app.core.log.Logger
import com.genesyx.app.core.result.DataResult
import com.genesyx.app.data.local.GenesyxDatabase
import com.genesyx.app.data.local.datastore.GenesyxPreferencesDataStore
import com.genesyx.app.data.local.entity.LogSyncStatus
import com.genesyx.app.data.remote.DailyLogRemoteDataSource
import com.genesyx.app.data.sync.DailyLogSyncScheduler
import com.genesyx.app.domain.model.DailyLog
import com.genesyx.app.domain.model.EnergyLevel
import com.genesyx.app.domain.model.Mood
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * The test that proves tracking is real: a genuine Room database, the generated DAO, and the real
 * [DailyLogRepository] — nothing about the store is mocked. Every other test in the suite hands the
 * repository a pre-built map, so none of them could catch a log that fails to persist, a set that
 * fails to serialise, or a row that reads back under the wrong user.
 *
 * Instrumented rather than JVM because Room needs an Android runtime and Robolectric would be a new
 * dependency. Run with: `./gradlew connectedDebugAndroidTest`.
 */
@RunWith(AndroidJUnit4::class)
class DailyLogRepositoryTest {

    private lateinit var db: GenesyxDatabase
    private lateinit var scope: CoroutineScope
    private lateinit var session: SessionRepository
    private lateinit var repo: DailyLogRepository
    private lateinit var remote: FakeRemote
    private lateinit var scheduler: RecordingScheduler

    private val today: LocalDate = LocalDate.now()

    /**
     * A fake server with a switch. While [online] is false every push fails, exactly as it does on a
     * plane; flipping it to true lets the queue drain. [serverLogs] is what a pull returns, so a test
     * can stage the "server disagrees with my offline edit" case that used to lose data.
     */
    private class FakeRemote : DailyLogRemoteDataSource {
        var online = false
        val serverLogs = mutableMapOf<LocalDate, DailyLog>()
        val pushed = mutableListOf<Pair<LocalDate, DailyLog>>()

        override suspend fun listLogs(userId: String) =
            if (online) DataResult.Success(serverLogs.toMap()) else DataResult.Error(offline)

        override suspend fun getLog(userId: String, date: LocalDate) =
            if (online) DataResult.Success(serverLogs[date]) else DataResult.Error(offline)

        override suspend fun upsertLog(userId: String, date: LocalDate, log: DailyLog) =
            if (online) {
                serverLogs[date] = log
                pushed += date to log
                DataResult.Success(Unit)
            } else {
                DataResult.Error(offline)
            }

        private companion object {
            val offline = java.io.IOException("offline")
        }
    }

    /** Records that a background retry was asked for, without dragging WorkManager into the test. */
    private class RecordingScheduler : DailyLogSyncScheduler {
        var scheduled = 0
        override fun schedule() { scheduled++ }
    }

    private object SilentLogger : Logger {
        override fun d(tag: String, message: String) = Unit
        override fun i(tag: String, message: String) = Unit
        override fun w(tag: String, message: String, throwable: Throwable?) = Unit
        override fun e(tag: String, message: String, throwable: Throwable?) = Unit
    }

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, GenesyxDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        scope = CoroutineScope(Dispatchers.IO)

        val prefsFile = context.filesDir.resolve("daily_log_repo_test_${System.nanoTime()}.preferences_pb")
        val ds: DataStore<Preferences> = PreferenceDataStoreFactory.create(produceFile = { prefsFile })
        session = SessionRepository(GenesyxPreferencesDataStore(ds), scope)

        remote = FakeRemote()
        scheduler = RecordingScheduler()
        repo = DailyLogRepository(db.dailyLogDao(), remote, session, scheduler, SilentLogger, scope)
    }

    @After
    fun tearDown() {
        scope.cancel()
        db.close()
    }

    /** Awaits a real Room emission rather than sampling the StateFlow's seed value. */
    private suspend fun logsWhen(predicate: (Map<LocalDate, DailyLog>) -> Boolean): Map<LocalDate, DailyLog> =
        withTimeout(AWAIT_MS) { repo.logByDate.first(predicate) }

    @Test
    fun a_log_saved_on_a_past_date_reads_back_for_that_date() = runBlocking {
        val past = today.minusDays(3)
        val written = DailyLog(
            mood = Mood.GOOD,
            energy = EnergyLevel.LOW,
            symptoms = setOf("Cramps", "Fatigue"),
            sleepMinutes = 450,
            supplements = setOf("Iron", "Folic acid"),
            notes = "Rough night.",
            waterMl = 1500,
        )

        repo.upsert(past, written)
        val logs = logsWhen { past in it }

        // Every field survives the round-trip — this is also the only coverage of the Room
        // TypeConverters that (de)serialise the symptom and supplement sets.
        val read = logs.getValue(past)
        assertEquals(Mood.GOOD, read.mood)
        assertEquals(EnergyLevel.LOW, read.energy)
        assertEquals(setOf("Cramps", "Fatigue"), read.symptoms)
        assertEquals(450, read.sleepMinutes)
        assertEquals(setOf("Iron", "Folic acid"), read.supplements)
        assertEquals("Rough night.", read.notes)
        assertEquals(1500, read.waterMl)

        // ...and it is filed under that date, not today.
        assertEquals(written, repo.logOn(past))
        assertEquals(DailyLog(), repo.logOn(today))
    }

    @Test
    fun upsert_on_the_same_date_replaces_rather_than_duplicating() = runBlocking {
        repo.upsert(today, DailyLog(waterMl = 500, mood = Mood.LOW))
        logsWhen { it[today]?.waterMl == 500 }

        repo.upsert(today, DailyLog(waterMl = 900, mood = Mood.GREAT))
        val logs = logsWhen { it[today]?.waterMl == 900 }

        // UNIQUE(user_id, date): one row per day, overwritten in place.
        assertEquals(1, logs.size)
        assertEquals(Mood.GREAT, logs.getValue(today).mood)
    }

    @Test
    fun loaded_is_true_once_room_answers_even_when_the_user_has_no_logs() = runBlocking {
        // The whole point of `loaded`: "no log today" must be distinguishable from "Room has not
        // replied yet". Without it, LogScreen seeded a blank form over a real log and saved zeros
        // over it. An empty database must still flip the flag.
        withTimeout(AWAIT_MS) { repo.loaded.first { it } }

        assertTrue(repo.loaded.value)
        assertTrue(repo.logByDate.value.isEmpty())
    }

    @Test
    fun water_helpers_persist_and_clamp() = runBlocking {
        repo.adjustWater(250, today)
        logsWhen { it[today]?.waterMl == 250 }

        repo.adjustWater(-1000, today) // would go negative
        logsWhen { it[today]?.waterMl == 0 }
        assertEquals(0, repo.waterMlOn(today))

        repo.setWater(99_999, today) // above the ceiling
        logsWhen { it[today]?.waterMl == 10_000 }
        assertEquals(10_000, repo.waterMlOn(today))
    }

    @Test
    fun a_signed_in_user_does_not_see_the_guest_bucket() = runBlocking {
        // Guest writes first, under LOCAL_USER_ID.
        repo.upsert(today, DailyLog(waterMl = 700))
        logsWhen { it[today]?.waterMl == 700 }

        // Signing in re-scopes the query to the account's rows — the guest's log must not follow.
        session.signIn(email = "b@example.com", name = "B", userId = "uid-b")
        val afterSignIn = logsWhen { today !in it }

        assertNull(afterSignIn[today])
        assertEquals(DailyLog(), repo.logOn(today))

        // B's own log is filed under B, and does not disturb the guest's row.
        repo.upsert(today, DailyLog(waterMl = 300))
        logsWhen { it[today]?.waterMl == 300 }
        assertEquals(1, db.dailyLogDao().observeAll("uid-b").first().size)
        assertEquals(1, db.dailyLogDao().observeAll(SessionRepository.LOCAL_USER_ID).first().size)
    }

    // ── the offline queue (v1.1) ──

    /**
     * Signs in and WAITS for it to land. `currentUserId()` reads a StateFlow fed asynchronously from
     * DataStore, so writing straight after signIn() files the row under the guest id instead.
     */
    private suspend fun signInAndAwait(userId: String) {
        session.signIn(email = "$userId@example.com", name = "A", userId = userId)
        withTimeout(AWAIT_MS) {
            while (session.currentUserId() != userId) kotlinx.coroutines.delay(20)
        }
    }

    /** Awaits the row's sync status, so we assert on committed state rather than racing the write. */
    private suspend fun statusWhen(date: LocalDate, want: LogSyncStatus): LogSyncStatus =
        withTimeout(AWAIT_MS) {
            var seen = db.dailyLogDao().getByDate(session.currentUserId(), date)?.syncStatus
            while (seen != want) {
                kotlinx.coroutines.delay(20)
                seen = db.dailyLogDao().getByDate(session.currentUserId(), date)?.syncStatus
            }
            seen!!
        }

    @Test
    fun an_offline_save_is_kept_and_queued_rather_than_refused() = runBlocking {
        signInAndAwait("uid-a")
        remote.online = false

        repo.upsert(today, DailyLog(waterMl = 800, mood = Mood.GOOD))

        // The write survives (v1.0 refused it outright) and is marked for the retry queue.
        assertEquals(LogSyncStatus.PENDING_UPSERT, statusWhen(today, LogSyncStatus.PENDING_UPSERT))
        assertEquals(800, logsWhen { it[today]?.waterMl == 800 }.getValue(today).waterMl)
        assertTrue("a failed push must ask for a background retry", scheduler.scheduled > 0)
    }

    @Test
    fun a_pull_must_not_overwrite_an_unsynced_local_edit() = runBlocking {
        signInAndAwait("uid-a")

        // She edits today's log on a plane: it lands locally as PENDING.
        remote.online = false
        repo.upsert(today, DailyLog(waterMl = 800, notes = "written offline"))
        statusWhen(today, LogSyncStatus.PENDING_UPSERT)

        // The server still holds the stale copy from before the flight.
        remote.serverLogs[today] = DailyLog(waterMl = 100, notes = "stale server copy")
        remote.online = true

        repo.refresh("uid-a")

        // Wait for the row to settle (refresh pulls, then drains the queue) before reading it.
        assertEquals(LogSyncStatus.SYNCED, statusWhen(today, LogSyncStatus.SYNCED))

        // THE regression: refresh used to stamp the server's row over hers, silently binning the
        // edit — the data loss that v1.0's "you're offline" block existed to avoid. Local wins,
        // and the queue pushes it up so the server agrees. Read the row from the DAO rather than the
        // cached StateFlow, which is emitted asynchronously and would make this assertion a race.
        val row = db.dailyLogDao().getByDate("uid-a", today)!!
        assertEquals("written offline", row.notes)
        assertEquals(800, row.waterMl)
        assertEquals(800, remote.serverLogs.getValue(today).waterMl)
        assertEquals("written offline", remote.serverLogs.getValue(today).notes)
    }

    @Test
    fun draining_the_queue_pushes_every_pending_row_and_marks_it_synced() = runBlocking {
        signInAndAwait("uid-a")

        remote.online = false
        repo.upsert(today, DailyLog(waterMl = 500))
        repo.upsert(today.minusDays(1), DailyLog(waterMl = 600))
        statusWhen(today, LogSyncStatus.PENDING_UPSERT)
        statusWhen(today.minusDays(1), LogSyncStatus.PENDING_UPSERT)

        remote.online = true // the plane lands
        assertTrue("a full drain reports success so WorkManager stops retrying", repo.syncPending())

        assertEquals(LogSyncStatus.SYNCED, statusWhen(today, LogSyncStatus.SYNCED))
        assertEquals(LogSyncStatus.SYNCED, statusWhen(today.minusDays(1), LogSyncStatus.SYNCED))
        assertEquals(500, remote.serverLogs.getValue(today).waterMl)
        assertEquals(600, remote.serverLogs.getValue(today.minusDays(1)).waterMl)
    }

    @Test
    fun a_guest_write_is_never_queued() = runBlocking {
        // Guests have no server row to sync to (RLS scopes to auth.uid()), so queueing one would
        // retry forever against nothing.
        remote.online = false
        repo.upsert(today, DailyLog(waterMl = 400))

        assertEquals(LogSyncStatus.SYNCED, statusWhen(today, LogSyncStatus.SYNCED))
        assertEquals(0, scheduler.scheduled)
        assertTrue(remote.pushed.isEmpty())
    }
    private companion object {
        /**
         * Generous on purpose. These helpers poll real Room/DataStore writes on real hardware, and the
         * first run after an install pays for dexopt — a 5s budget made them flaky rather than strict.
         * A passing test still returns in milliseconds; only a genuine hang waits this long.
         */
        const val AWAIT_MS = 20_000L
    }

}
