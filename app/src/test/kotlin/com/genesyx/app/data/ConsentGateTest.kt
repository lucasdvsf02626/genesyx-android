package com.genesyx.app.data

import app.cash.turbine.test
import com.genesyx.app.core.log.Logger
import com.genesyx.app.data.local.dao.CycleSettingsDao
import com.genesyx.app.data.local.dao.DailyLogDao
import com.genesyx.app.data.local.dao.PhReadingDao
import com.genesyx.app.data.local.datastore.GenesyxPreferencesDataStore
import com.genesyx.app.data.local.entity.PhReadingEntity
import com.genesyx.app.data.remote.CycleRemoteDataSource
import com.genesyx.app.data.remote.DailyLogRemoteDataSource
import com.genesyx.app.data.remote.PhRemoteDataSource
import com.genesyx.app.data.remote.QuizAnswersRemoteDataSource
import com.genesyx.app.data.sync.DailyLogSyncScheduler
import com.genesyx.app.data.sync.PhSyncScheduler
import com.genesyx.app.domain.consent.HealthDataCollectionGate
import com.genesyx.app.domain.model.CycleSettings
import com.genesyx.app.domain.model.DailyLog
import com.genesyx.app.domain.model.PhReading
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Article 9 gating across the four health stores.
 *
 * Reads are asserted as hard as writes. Gating only the writes is what voided the equivalent iOS
 * build: after a withdrawal there is no lawful basis to pull her health data back down, and a pull
 * is also what makes a withdrawal look like it never happened — the server's copy lands back on the
 * device and the next screen shows it.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ConsentGateTest {

    private val withdrawn = HealthDataCollectionGate { false }
    private val logger = mockk<Logger>(relaxed = true)

    private fun signedInSession(): SessionRepository = mockk<SessionRepository>().also {
        every { it.userId } returns MutableStateFlow<String?>("user-a")
        every { it.currentUserId() } returns "user-a"
        coEvery { it.awaitUserId() } returns "user-a"
        every { it.isSignedIn } returns MutableStateFlow(true)
    }

    // ── pH ────────────────────────────────────────────────────────────────────────────────────

    @Test
    fun `a pH reading is not written while consent is withdrawn`() = runTest {
        val dao = mockk<PhReadingDao>(relaxed = true)
        val remote = mockk<PhRemoteDataSource>(relaxed = true)
        every { dao.observeAll(any()) } returns flowOf(emptyList())
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))

        PhRepository(
            dao, remote, signedInSession(), mockk<PhSyncScheduler>(relaxed = true), logger, scope, withdrawn,
        ).create(PhReading(id = "r1", phValue = 4.2, recordedAt = LocalDateTime.now()))

        coVerify(exactly = 0) { dao.upsert(any()) }
        coVerify(exactly = 0) { remote.upsert(any()) }
        scope.cancel()
    }

    /**
     * The gate is a suspend call, and the write used to run it inside `scope.launch` after already
     * returning Accepted — so the log dialog closed on a success the reading never had. Nothing
     * distinguishes a refusal from a save unless the result carries it.
     */
    @Test
    fun `a refused pH write says so instead of reporting success`() = runTest {
        val dao = mockk<PhReadingDao>(relaxed = true)
        every { dao.observeAll(any()) } returns flowOf(emptyList())
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))

        val result = PhRepository(
            dao, mockk(relaxed = true), signedInSession(),
            mockk<PhSyncScheduler>(relaxed = true), logger, scope, withdrawn,
        ).create(PhReading(id = "r1", phValue = 4.2, recordedAt = LocalDateTime.now()))

        assertEquals(PhWriteResult.Refused, result)
        scope.cancel()
    }

    @Test
    fun `pH is not pulled back down while consent is withdrawn`() = runTest {
        val dao = mockk<PhReadingDao>(relaxed = true)
        val remote = mockk<PhRemoteDataSource>(relaxed = true)
        every { dao.observeAll(any()) } returns flowOf(emptyList())
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))

        PhRepository(
            dao, remote, signedInSession(), mockk<PhSyncScheduler>(relaxed = true), logger, scope, withdrawn,
        ).refresh()

        coVerify(exactly = 0) { remote.list(any()) }
        coVerify(exactly = 0) { dao.upsert(any()) }
        scope.cancel()
    }

    /**
     * Withdrawal stops collection. It is not an erasure request, so the readings she already took
     * stay readable — this is the invariant that keeps "pause tracking" from costing her history.
     */
    @Test
    fun `withdrawing keeps the readings she already recorded`() = runTest {
        val existing = PhReadingEntity("r1", "user-a", 4.2, LocalDateTime.of(2026, 1, 1, 9, 0), null)
        val dao = mockk<PhReadingDao>(relaxed = true)
        val remote = mockk<PhRemoteDataSource>(relaxed = true)
        every { dao.observeAll(any()) } returns flowOf(listOf(existing))
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))

        val repo = PhRepository(
            dao, remote, signedInSession(), mockk<PhSyncScheduler>(relaxed = true), logger, scope, withdrawn,
        )

        repo.readings.test {
            assertEquals(listOf("r1"), awaitItem().map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
        coVerify(exactly = 0) { dao.markDeleted(any(), any()) }
        scope.cancel()
    }

    // ── Daily logs ────────────────────────────────────────────────────────────────────────────

    @Test
    fun `a daily log is not written while consent is withdrawn`() = runTest {
        val dao = mockk<DailyLogDao>(relaxed = true)
        val remote = mockk<DailyLogRemoteDataSource>(relaxed = true)
        every { dao.observeAll(any()) } returns flowOf(emptyList())
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))

        DailyLogRepository(
            dao, remote, signedInSession(), mockk<DailyLogSyncScheduler>(relaxed = true), logger, scope, withdrawn,
        ).upsert(LocalDate.of(2026, 1, 1), DailyLog())

        coVerify(exactly = 0) { dao.upsert(any()) }
        coVerify(exactly = 0) { remote.upsertLog(any(), any(), any()) }
        scope.cancel()
    }

    /** The quick-add trackers go through [DailyLogRepository.mutateRow], a different path to `upsert`. */
    @Test
    fun `a hydration quick-add is refused while consent is withdrawn`() = runTest {
        val dao = mockk<DailyLogDao>(relaxed = true)
        val remote = mockk<DailyLogRemoteDataSource>(relaxed = true)
        every { dao.observeAll(any()) } returns flowOf(emptyList())
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))

        DailyLogRepository(
            dao, remote, signedInSession(), mockk<DailyLogSyncScheduler>(relaxed = true), logger, scope, withdrawn,
        ).adjustWater(200, LocalDate.of(2026, 1, 1))

        coVerify(exactly = 0) { dao.upsert(any()) }
        scope.cancel()
    }

    @Test
    fun `daily logs are not pulled back down while consent is withdrawn`() = runTest {
        val dao = mockk<DailyLogDao>(relaxed = true)
        val remote = mockk<DailyLogRemoteDataSource>(relaxed = true)
        every { dao.observeAll(any()) } returns flowOf(emptyList())
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))

        DailyLogRepository(
            dao, remote, signedInSession(), mockk<DailyLogSyncScheduler>(relaxed = true), logger, scope, withdrawn,
        ).refresh()

        coVerify(exactly = 0) { remote.listLogs(any()) }
        scope.cancel()
    }

    // ── Cycle ─────────────────────────────────────────────────────────────────────────────────

    @Test
    fun `cycle settings are not written while consent is withdrawn`() = runTest {
        val dao = mockk<CycleSettingsDao>(relaxed = true)
        val remote = mockk<CycleRemoteDataSource>(relaxed = true)
        every { dao.observe(any()) } returns flowOf(null)
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))

        CycleRepository(dao, remote, signedInSession(), logger, scope, withdrawn)
            .upsert(CycleSettings(LocalDate.of(2026, 1, 1), 28, 5))

        coVerify(exactly = 0) { dao.upsert(any()) }
        coVerify(exactly = 0) { remote.upsertCycleSettings(any(), any()) }
        scope.cancel()
    }

    @Test
    fun `cycle settings are not pulled back down while consent is withdrawn`() = runTest {
        val dao = mockk<CycleSettingsDao>(relaxed = true)
        val remote = mockk<CycleRemoteDataSource>(relaxed = true)
        every { dao.observe(any()) } returns flowOf(null)
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))

        CycleRepository(dao, remote, signedInSession(), logger, scope, withdrawn).refresh()

        coVerify(exactly = 0) { remote.getCycleSettings(any()) }
        coVerify(exactly = 0) { dao.upsert(any()) }
        scope.cancel()
    }

    // ── Tracking answers ──────────────────────────────────────────────────────────────────────

    @Test
    fun `tracking answers are not recorded or pulled while consent is withdrawn`() = runTest {
        val store = mockk<GenesyxPreferencesDataStore>(relaxed = true)
        val remote = mockk<QuizAnswersRemoteDataSource>(relaxed = true)
        every { store.quizAnswers } returns flowOf(emptyMap())
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))

        val repo = QuizAnswersRepository(store, remote, signedInSession(), scope, logger, withdrawn)
        repo.record(mapOf("stage" to "trying"))
        repo.refresh("user-a")

        coVerify(exactly = 0) { store.setQuizAnswers(any()) }
        coVerify(exactly = 0) { remote.get(any()) }
        coVerify(exactly = 0) { remote.upsert(any(), any()) }
        scope.cancel()
    }
}
