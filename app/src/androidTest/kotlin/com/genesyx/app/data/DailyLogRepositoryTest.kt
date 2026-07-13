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
import com.genesyx.app.data.remote.DailyLogRemoteDataSource
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

    private val today: LocalDate = LocalDate.now()

    /** Offline: every push fails, so nothing here depends on a network. Room is the source of truth. */
    private class OfflineRemote : DailyLogRemoteDataSource {
        override suspend fun listLogs(userId: String) = DataResult.Error(offline)
        override suspend fun getLog(userId: String, date: LocalDate) = DataResult.Error(offline)
        override suspend fun upsertLog(userId: String, date: LocalDate, log: DailyLog) = DataResult.Error(offline)

        private companion object {
            val offline = java.io.IOException("offline")
        }
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

        repo = DailyLogRepository(db.dailyLogDao(), OfflineRemote(), session, SilentLogger, scope)
    }

    @After
    fun tearDown() {
        scope.cancel()
        db.close()
    }

    /** Awaits a real Room emission rather than sampling the StateFlow's seed value. */
    private suspend fun logsWhen(predicate: (Map<LocalDate, DailyLog>) -> Boolean): Map<LocalDate, DailyLog> =
        withTimeout(5_000) { repo.logByDate.first(predicate) }

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
        withTimeout(5_000) { repo.loaded.first { it } }

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
}
