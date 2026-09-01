package com.genesyx.app.data

import com.genesyx.app.core.log.Logger
import com.genesyx.app.core.result.DataResult
import com.genesyx.app.data.local.dao.CycleSettingsDao
import com.genesyx.app.data.local.datastore.GenesyxPreferencesDataStore
import com.genesyx.app.data.local.entity.toEntity
import com.genesyx.app.data.remote.CycleRemoteDataSource
import com.genesyx.app.domain.model.CycleSettings
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.LocalDate

/**
 * The cycle-settings owed-write contract (audit P1 #6) and guest adoption (P1 #7). Cycle settings
 * were the one store where an offline edit could be silently overwritten by the sign-in pull —
 * these tests pin the push-before-pull order and the abort-on-failed-push rule that quiz answers
 * and the display name already follow.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CycleSettingsSyncTest {

    private val logger = mockk<Logger>(relaxed = true)
    private val local = CycleSettings(LocalDate.of(2026, 2, 1), 30, 6)
    private val server = CycleSettings(LocalDate.of(2026, 1, 1), 28, 5)

    private fun session(): SessionRepository = mockk<SessionRepository>().also {
        every { it.userId } returns MutableStateFlow<String?>("user-a")
        every { it.currentUserId() } returns "user-a"
        coEvery { it.awaitUserId() } returns "user-a"
        every { it.isSignedIn } returns MutableStateFlow(true)
    }

    private fun repo(
        scope: CoroutineScope,
        dao: CycleSettingsDao,
        remote: CycleRemoteDataSource,
        store: GenesyxPreferencesDataStore,
    ): CycleRepository {
        every { dao.observe(any()) } returns flowOf(null)
        return CycleRepository(dao, remote, session(), store, logger, scope)
    }

    @Test
    fun `an owed offline edit is pushed before the pull, so the server cannot clobber it`() = runTest {
        val dao = mockk<CycleSettingsDao>(relaxed = true)
        val remote = mockk<CycleRemoteDataSource>(relaxed = true)
        val store = mockk<GenesyxPreferencesDataStore>(relaxed = true)
        every { store.cycleSettingsOwed } returns flowOf(true)
        coEvery { dao.get("user-a") } returns local.toEntity("user-a")
        coEvery { remote.upsertCycleSettings(any(), any()) } returns DataResult.Success(Unit)
        coEvery { remote.getCycleSettings("user-a") } returns DataResult.Success(local)
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))

        repo(scope, dao, remote, store).refresh("user-a")

        coVerifyOrder {
            remote.upsertCycleSettings("user-a", local) // the debt goes first
            remote.getCycleSettings("user-a")
        }
        coVerify { store.setCycleSettingsOwed(false) }
        scope.cancel()
    }

    @Test
    fun `a failed owed push aborts the pull rather than pulling over the local copy`() = runTest {
        // This is the audited defect: refresh used to upsert the server row unconditionally, so an
        // offline edit whose push had failed was silently replaced by the stale server copy.
        val dao = mockk<CycleSettingsDao>(relaxed = true)
        val remote = mockk<CycleRemoteDataSource>(relaxed = true)
        val store = mockk<GenesyxPreferencesDataStore>(relaxed = true)
        every { store.cycleSettingsOwed } returns flowOf(true)
        coEvery { dao.get("user-a") } returns local.toEntity("user-a")
        coEvery { remote.upsertCycleSettings(any(), any()) } returns
            DataResult.Error(IllegalStateException("offline"), "offline")
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))

        repo(scope, dao, remote, store).refresh("user-a")

        coVerify(exactly = 0) { remote.getCycleSettings(any()) }
        coVerify(exactly = 0) { dao.upsert(any()) }
        coVerify(exactly = 0) { store.setCycleSettingsOwed(false) } // still owed
        scope.cancel()
    }

    @Test
    fun `a failed save marks the row owed so the next refresh retries it`() = runTest {
        val dao = mockk<CycleSettingsDao>(relaxed = true)
        val remote = mockk<CycleRemoteDataSource>(relaxed = true)
        val store = mockk<GenesyxPreferencesDataStore>(relaxed = true)
        coEvery { remote.upsertCycleSettings(any(), any()) } returns
            DataResult.Error(IllegalStateException("offline"), "offline")
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))

        repo(scope, dao, remote, store).upsert(local)

        coVerify { store.setCycleSettingsOwed(true) }
        coVerify(exactly = 0) { store.setCycleSettingsOwed(false) }
        scope.cancel()
    }

    @Test
    fun `guest cycle settings are adopted onto the account at sign-in`() = runTest {
        val dao = mockk<CycleSettingsDao>(relaxed = true)
        val remote = mockk<CycleRemoteDataSource>(relaxed = true)
        val store = mockk<GenesyxPreferencesDataStore>(relaxed = true)
        coEvery { dao.get("user-a") } returns null
        coEvery { dao.get(SessionRepository.LOCAL_USER_ID) } returns
            local.toEntity(SessionRepository.LOCAL_USER_ID)
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))

        repo(scope, dao, remote, store).adoptGuestSettings("user-a")

        coVerify { dao.upsert(local.toEntity("user-a")) }
        scope.cancel()
    }

    @Test
    fun `an account that already has settings does not adopt the guest's`() = runTest {
        val dao = mockk<CycleSettingsDao>(relaxed = true)
        val remote = mockk<CycleRemoteDataSource>(relaxed = true)
        val store = mockk<GenesyxPreferencesDataStore>(relaxed = true)
        coEvery { dao.get("user-a") } returns server.toEntity("user-a")
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))

        repo(scope, dao, remote, store).adoptGuestSettings("user-a")

        coVerify(exactly = 0) { dao.upsert(any()) }
        scope.cancel()
    }

    @Test
    fun `adopted settings reach the server when it has no row, and a server row wins when it does`() = runTest {
        val dao = mockk<CycleSettingsDao>(relaxed = true)
        val remote = mockk<CycleRemoteDataSource>(relaxed = true)
        val store = mockk<GenesyxPreferencesDataStore>(relaxed = true)
        every { store.cycleSettingsOwed } returns flowOf(false)
        coEvery { dao.get("user-a") } returns local.toEntity("user-a")
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))

        // Server empty → the adopted local copy is pushed (quiz-answers rule).
        coEvery { remote.getCycleSettings("user-a") } returns DataResult.Success(null)
        repo(scope, dao, remote, store).refresh("user-a")
        coVerify { remote.upsertCycleSettings("user-a", local) }

        // Server row present → it is cached over the local copy.
        coEvery { remote.getCycleSettings("user-a") } returns DataResult.Success(server)
        repo(scope, dao, remote, store).refresh("user-a")
        coVerify { dao.upsert(server.toEntity("user-a")) }
        scope.cancel()
    }
}
