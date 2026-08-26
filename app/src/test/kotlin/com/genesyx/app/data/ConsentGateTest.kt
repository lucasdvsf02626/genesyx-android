package com.genesyx.app.data

import app.cash.turbine.test
import com.genesyx.app.core.log.Logger
import com.genesyx.app.core.result.DataResult
import com.genesyx.app.data.local.dao.CycleSettingsDao
import com.genesyx.app.data.local.dao.DailyLogDao
import com.genesyx.app.data.local.dao.PhReadingDao
import com.genesyx.app.data.local.dao.UserSupplementDao
import com.genesyx.app.data.local.datastore.GenesyxPreferencesDataStore
import com.genesyx.app.data.local.entity.PhReadingEntity
import com.genesyx.app.data.local.entity.SupplementSyncStatus
import com.genesyx.app.data.local.entity.UserSupplementEntity
import com.genesyx.app.data.remote.CycleRemoteDataSource
import com.genesyx.app.data.remote.DailyLogRemoteDataSource
import com.genesyx.app.data.remote.PhRemoteDataSource
import com.genesyx.app.data.remote.QuizAnswersRemoteDataSource
import com.genesyx.app.data.remote.UserSupplementRemoteDataSource
import com.genesyx.app.data.sync.DailyLogSyncScheduler
import com.genesyx.app.data.sync.PhSyncScheduler
import com.genesyx.app.data.sync.UserSupplementSyncScheduler
import com.genesyx.app.domain.consent.HealthDataCollectionGate
import com.genesyx.app.domain.model.CycleSettings
import com.genesyx.app.domain.model.DailyLog
import com.genesyx.app.domain.model.PhReading
import com.genesyx.app.domain.model.UserSupplement
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
 * Article 9 gating across the five health stores.
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

    // ── Her own supplements ───────────────────────────────────────────────────────────────────
    //
    // What she takes is health data and it syncs to Supabase, but this repository shipped without
    // the gate the other four had — it was cloned from PhRepository before the gate existed.

    private fun supplementRepo(
        dao: UserSupplementDao,
        remote: UserSupplementRemoteDataSource,
        scheduler: UserSupplementSyncScheduler,
        scope: CoroutineScope,
    ): UserSupplementRepository {
        every { dao.observeAll(any()) } returns flowOf(emptyList())
        return UserSupplementRepository(dao, remote, signedInSession(), scheduler, logger, scope, withdrawn)
    }

    @Test
    fun `a supplement is not written while consent is withdrawn, and says so`() = runTest {
        val dao = mockk<UserSupplementDao>(relaxed = true)
        val remote = mockk<UserSupplementRemoteDataSource>(relaxed = true)
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))

        val result = supplementRepo(dao, remote, mockk(relaxed = true), scope)
            .create(UserSupplement(id = "s1", name = "Magnesium"))

        assertEquals(SupplementWriteResult.Refused, result)
        coVerify(exactly = 0) { dao.upsert(any()) }
        coVerify(exactly = 0) { remote.upsert(any()) }
        scope.cancel()
    }

    @Test
    fun `supplements are not pulled back down while consent is withdrawn`() = runTest {
        val dao = mockk<UserSupplementDao>(relaxed = true)
        val remote = mockk<UserSupplementRemoteDataSource>(relaxed = true)
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))

        supplementRepo(dao, remote, mockk(relaxed = true), scope).refresh("user-a")

        coVerify(exactly = 0) { remote.list(any()) }
        coVerify(exactly = 0) { dao.upsert(any()) }
        scope.cancel()
    }

    /**
     * The queue is the leak the write gate alone does not close: rows already marked PENDING_UPSERT
     * when she withdrew would otherwise be uploaded by the next worker run. Draining reports success
     * so WorkManager retires the job instead of retrying a refusal until backoff gives up.
     */
    @Test
    fun `a queued supplement is not uploaded after a withdrawal`() = runTest {
        val dao = mockk<UserSupplementDao>(relaxed = true)
        val remote = mockk<UserSupplementRemoteDataSource>(relaxed = true)
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))

        val drained = supplementRepo(dao, remote, mockk(relaxed = true), scope).syncPending()

        assertEquals(true, drained)
        coVerify(exactly = 0) { dao.pending() }
        coVerify(exactly = 0) { remote.upsert(any()) }
        scope.cancel()
    }

    @Test
    fun `guest supplements are not adopted into an account while consent is withdrawn`() = runTest {
        val dao = mockk<UserSupplementDao>(relaxed = true)
        val scheduler = mockk<UserSupplementSyncScheduler>(relaxed = true)
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))

        val adopted = supplementRepo(dao, mockk(relaxed = true), scheduler, scope)
            .adoptGuestEntries("user-a")

        assertEquals(0, adopted)
        coVerify(exactly = 0) { dao.adoptGuestRows(any(), any(), any()) }
        verify(exactly = 0) { scheduler.schedule() }
        scope.cancel()
    }

    /**
     * Deletion is deliberately outside the gate, here and in [PhRepository]. Withdrawal stops
     * collection; refusing to propagate a removal would strand her row on the server — the opposite
     * of what she asked for.
     */
    @Test
    fun `deleting a supplement still reaches the server while consent is withdrawn`() = runTest {
        val dao = mockk<UserSupplementDao>(relaxed = true)
        val remote = mockk<UserSupplementRemoteDataSource>(relaxed = true)
        coEvery { dao.getById("s1") } returns UserSupplementEntity(
            id = "s1", userId = "user-a", name = "Magnesium", dose = null, timeOfDay = null,
            productId = null, createdAt = LocalDateTime.of(2026, 1, 1, 9, 0),
            syncStatus = SupplementSyncStatus.PENDING_DELETE,
        )
        coEvery { remote.upsert(any()) } returns DataResult.Success(Unit)
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))

        supplementRepo(dao, remote, mockk(relaxed = true), scope).delete("s1")

        coVerify { dao.markDeleted(eq("s1"), any()) }
        coVerify(exactly = 1) { remote.upsert(any()) }
        scope.cancel()
    }
}
