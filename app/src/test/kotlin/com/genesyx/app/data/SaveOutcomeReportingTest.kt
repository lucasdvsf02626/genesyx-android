package com.genesyx.app.data

import com.genesyx.app.core.log.Logger
import com.genesyx.app.core.result.DataResult
import com.genesyx.app.core.result.SaveOutcome
import com.genesyx.app.data.local.dao.CycleSettingsDao
import com.genesyx.app.data.local.datastore.GenesyxPreferencesDataStore
import com.genesyx.app.data.remote.CycleRemoteDataSource
import com.genesyx.app.data.remote.QuizAnswersRemoteDataSource
import com.genesyx.app.domain.consent.HealthDataCollectionGate
import com.genesyx.app.domain.model.CycleSettings
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
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * A save that was refused and a save that broke are different events, and these pin that they stay
 * different all the way to the caller.
 *
 * Collapsing them is the defect being ported away from: when both arrive as one "didn't work", the
 * only copy the editor can honestly show is generic, so a woman who withdrew consent is told to
 * check her connection and goes looking for a fault that isn't there.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SaveOutcomeReportingTest {

    private val logger = mockk<Logger>(relaxed = true)
    private val settings = CycleSettings(LocalDate.of(2026, 1, 1), 28, 5)

    private fun session(signedIn: Boolean = true): SessionRepository = mockk<SessionRepository>().also {
        every { it.userId } returns MutableStateFlow<String?>("user-a")
        every { it.currentUserId() } returns "user-a"
        coEvery { it.awaitUserId() } returns "user-a"
        every { it.isSignedIn } returns MutableStateFlow(signedIn)
    }

    // ── Cycle settings ────────────────────────────────────────────────────────────────────────

    private fun cycleRepo(
        scope: CoroutineScope,
        remote: CycleRemoteDataSource,
        dao: CycleSettingsDao,
        consent: HealthDataCollectionGate,
    ): CycleRepository {
        every { dao.observe(any()) } returns flowOf(null)
        val store = mockk<com.genesyx.app.data.local.datastore.GenesyxPreferencesDataStore>(relaxed = true)
        return CycleRepository(dao, remote, session(), store, logger, scope, consent)
    }

    @Test
    fun `a refused cycle save says refused, and writes nothing`() = runTest {
        val dao = mockk<CycleSettingsDao>(relaxed = true)
        val remote = mockk<CycleRemoteDataSource>(relaxed = true)
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))

        val outcome = cycleRepo(scope, remote, dao, HealthDataCollectionGate { false }).upsert(settings)

        assertEquals(SaveOutcome.Refused, outcome)
        coVerify(exactly = 0) { dao.upsert(any()) }
        scope.cancel()
    }

    /**
     * The local write landed even though the push didn't — Room is the source of truth here. The
     * copy that hangs off [SaveOutcome.Failed] has to say so, so this asserts the local write
     * happened rather than only that the outcome was Failed.
     */
    @Test
    fun `a cycle save the server refuses says failed, and keeps the local write`() = runTest {
        val dao = mockk<CycleSettingsDao>(relaxed = true)
        val remote = mockk<CycleRemoteDataSource>(relaxed = true)
        coEvery { remote.upsertCycleSettings(any(), any()) } returns
            DataResult.Error(IllegalStateException("offline"), "offline")
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))

        val outcome = cycleRepo(scope, remote, dao, HealthDataCollectionGate { true }).upsert(settings)

        assertTrue(outcome is SaveOutcome.Failed)
        coVerify(exactly = 1) { dao.upsert(any()) }
        scope.cancel()
    }

    @Test
    fun `a cycle save that reaches the server says saved`() = runTest {
        val dao = mockk<CycleSettingsDao>(relaxed = true)
        val remote = mockk<CycleRemoteDataSource>(relaxed = true)
        coEvery { remote.upsertCycleSettings(any(), any()) } returns DataResult.Success(Unit)
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))

        val outcome = cycleRepo(scope, remote, dao, HealthDataCollectionGate { true }).upsert(settings)

        assertEquals(SaveOutcome.Saved, outcome)
        scope.cancel()
    }

    // ── Tracking answers ──────────────────────────────────────────────────────────────────────

    private fun quizRepo(
        scope: CoroutineScope,
        remote: QuizAnswersRemoteDataSource,
        store: GenesyxPreferencesDataStore,
        consent: HealthDataCollectionGate,
        signedIn: Boolean = true,
    ): QuizAnswersRepository {
        every { store.quizAnswers } returns flowOf(emptyMap())
        return QuizAnswersRepository(store, remote, session(signedIn), scope, logger, consent)
    }

    @Test
    fun `refused tracking answers say refused, and write nothing`() = runTest {
        val store = mockk<GenesyxPreferencesDataStore>(relaxed = true)
        val remote = mockk<QuizAnswersRemoteDataSource>(relaxed = true)
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))

        val outcome = quizRepo(scope, remote, store, HealthDataCollectionGate { false })
            .record(mapOf("stage" to "trying"))

        assertEquals(SaveOutcome.Refused, outcome)
        coVerify(exactly = 0) { store.setQuizAnswers(any()) }
        scope.cancel()
    }

    @Test
    fun `tracking answers the server refuses say failed`() = runTest {
        val store = mockk<GenesyxPreferencesDataStore>(relaxed = true)
        val remote = mockk<QuizAnswersRemoteDataSource>(relaxed = true)
        coEvery { remote.upsert(any(), any()) } returns
            DataResult.Error(IllegalStateException("offline"), "offline")
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))

        val outcome = quizRepo(scope, remote, store, HealthDataCollectionGate { true })
            .record(mapOf("stage" to "trying"))

        assertTrue(outcome is SaveOutcome.Failed)
        coVerify(exactly = 1) { store.setQuizAnswers(any()) }
        scope.cancel()
    }

    /**
     * A guest has no server row to push to, so the local write IS the save. Reporting anything but
     * success would put an error in front of someone whose answers were stored exactly as intended.
     */
    @Test
    fun `a guest's tracking answers say saved without touching the server`() = runTest {
        val store = mockk<GenesyxPreferencesDataStore>(relaxed = true)
        val remote = mockk<QuizAnswersRemoteDataSource>(relaxed = true)
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))

        val outcome = quizRepo(scope, remote, store, HealthDataCollectionGate { true }, signedIn = false)
            .record(mapOf("stage" to "trying"))

        assertEquals(SaveOutcome.Saved, outcome)
        coVerify(exactly = 1) { store.setQuizAnswers(any()) }
        coVerify(exactly = 0) { remote.upsert(any(), any()) }
        scope.cancel()
    }
}
