package com.genesyx.app.data

import com.genesyx.app.core.log.Logger
import com.genesyx.app.core.result.DataResult
import com.genesyx.app.data.local.dao.UserSupplementDao
import com.genesyx.app.data.local.entity.SupplementSyncStatus
import com.genesyx.app.data.local.entity.UserSupplementEntity
import com.genesyx.app.data.remote.UserSupplementRemoteDataSource
import com.genesyx.app.data.remote.dto.UserSupplementDto
import com.genesyx.app.data.sync.UserSupplementSyncScheduler
import com.genesyx.app.domain.model.SupplementTime
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
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class UserSupplementRepositoryTest {

    private val dao = mockk<UserSupplementDao>(relaxed = true)
    private val remote = mockk<UserSupplementRemoteDataSource>(relaxed = true)
    private val scheduler = mockk<UserSupplementSyncScheduler>(relaxed = true)
    private val logger = mockk<Logger>(relaxed = true)
    private val session = mockk<SessionRepository>()

    private fun entity(id: String, uid: String, name: String) = UserSupplementEntity(
        id = id, userId = uid, name = name, dose = null, timeOfDay = null, productId = null,
        createdAt = LocalDateTime.of(2026, 1, 1, 9, 0),
    )

    private fun entry(name: String = "Magnesium") =
        UserSupplement(id = "s1", name = name, dose = "200 mg", timeOfDay = SupplementTime.EVENING)

    private fun TestScopeSetup(userId: String?) {
        every { session.userId } returns MutableStateFlow(userId)
        every { session.currentUserId() } returns (userId ?: SessionRepository.LOCAL_USER_ID)
        every { dao.observeAll(any()) } returns flowOf(emptyList())
    }

    @Test
    fun `online create writes PENDING then marks SYNCED, no retry queued`() = runTest {
        TestScopeSetup("user-a")
        coEvery { remote.upsert(any()) } returns DataResult.Success(Unit)
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))

        val result = UserSupplementRepository(dao, remote, session, scheduler, logger, scope)
            .create(entry())

        assertEquals(SupplementWriteResult.Accepted, result)
        coVerify {
            dao.upsert(
                match {
                    it.id == "s1" && it.syncStatus == SupplementSyncStatus.PENDING_UPSERT &&
                        it.timeOfDay == "evening"
                },
            )
        }
        coVerify { dao.setStatus("s1", SupplementSyncStatus.SYNCED) }   // pushed through
        verify(exactly = 0) { scheduler.schedule() }
        scope.cancel()
    }

    @Test
    fun `guest write stays local-only (no remote push, no WorkManager enqueue)`() = runTest {
        TestScopeSetup(null) // not signed in
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))

        val result = UserSupplementRepository(dao, remote, session, scheduler, logger, scope)
            .create(entry())

        assertEquals(SupplementWriteResult.Accepted, result)
        coVerify {
            dao.upsert(
                match { it.userId == SessionRepository.LOCAL_USER_ID && it.syncStatus == SupplementSyncStatus.SYNCED },
            )
        }
        coVerify(exactly = 0) { remote.upsert(any()) } // no server target for a guest
        verify(exactly = 0) { scheduler.schedule() }   // no doomed retry queued
        scope.cancel()
    }

    @Test
    fun `offline create stays PENDING and enqueues a retry (never blocks)`() = runTest {
        TestScopeSetup("user-a")
        coEvery { remote.upsert(any()) } returns DataResult.Error(RuntimeException("offline"))
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))

        val result = UserSupplementRepository(dao, remote, session, scheduler, logger, scope)
            .create(entry())

        assertEquals(SupplementWriteResult.Accepted, result)
        coVerify { dao.upsert(match { it.syncStatus == SupplementSyncStatus.PENDING_UPSERT }) }
        verify(exactly = 1) { scheduler.schedule() }
        coVerify(exactly = 0) { dao.setStatus("s1", SupplementSyncStatus.SYNCED) }
        scope.cancel()
    }

    @Test
    fun `blank and over-length names are rejected, never persisted`() = runTest {
        TestScopeSetup("user-a")
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        val repo = UserSupplementRepository(dao, remote, session, scheduler, logger, scope)

        assertEquals(SupplementWriteResult.InvalidName, repo.create(entry(name = "   ")))
        assertEquals(SupplementWriteResult.InvalidName, repo.create(entry(name = "x".repeat(61))))

        coVerify(exactly = 0) { dao.upsert(any()) }
        scope.cancel()
    }

    @Test
    fun `name and dose are trimmed on write, per the server contract`() = runTest {
        TestScopeSetup("user-a")
        coEvery { remote.upsert(any()) } returns DataResult.Success(Unit)
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))

        UserSupplementRepository(dao, remote, session, scheduler, logger, scope)
            .create(UserSupplement(id = "s1", name = "  Magnesium  ", dose = "  "))

        // Whitespace-only dose collapses to null, not an empty string on the server.
        coVerify { dao.upsert(match { it.name == "Magnesium" && it.dose == null }) }
        scope.cancel()
    }

    @Test
    fun `delete soft-deletes (tombstone) and pushes it`() = runTest {
        TestScopeSetup("user-a")
        coEvery { dao.getById("s1") } returns
            entity("s1", "user-a", "Magnesium").copy(syncStatus = SupplementSyncStatus.PENDING_DELETE)
        coEvery { remote.upsert(any()) } returns DataResult.Success(Unit)
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))

        UserSupplementRepository(dao, remote, session, scheduler, logger, scope).delete("s1")

        coVerify { dao.markDeleted(eq("s1"), any()) }
        coVerify { remote.upsert(any()) }
        scope.cancel()
    }

    @Test
    fun `refresh merges server rows by id and keeps locally-pending edits`() = runTest {
        every { session.userId } returns MutableStateFlow<String?>("user-a")
        every { dao.observeAll(any()) } returns flowOf(emptyList())
        coEvery { dao.pending() } returns emptyList()
        coEvery { dao.getById("srv") } returns null   // new from server
        coEvery { dao.getById("loc") } returns
            entity("loc", "user-a", "Iron").copy(syncStatus = SupplementSyncStatus.PENDING_UPSERT)
        coEvery { remote.list("user-a") } returns DataResult.Success(
            listOf(
                UserSupplementDto("srv", "user-a", "Folate", updatedAt = "2026-01-01T09:00:00"),
                UserSupplementDto("loc", "user-a", "SERVER STALE NAME", updatedAt = "2026-01-02T09:00:00"),
            ),
        )
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))

        UserSupplementRepository(dao, remote, session, scheduler, logger, scope).refresh("user-a")

        coVerify(exactly = 1) { dao.upsert(match { it.id == "srv" }) }        // pulled in once
        coVerify(exactly = 0) { dao.upsert(match { it.id == "loc" }) }        // local edit wins
        scope.cancel()
    }

    @Test
    fun `sign-in adopts guest entries and schedules a push`() = runTest {
        TestScopeSetup("user-a")
        coEvery { dao.adoptGuestRows(SessionRepository.LOCAL_USER_ID, "user-a", any()) } returns 2
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))

        val adopted = UserSupplementRepository(dao, remote, session, scheduler, logger, scope)
            .adoptGuestEntries("user-a")

        assertEquals(2, adopted)
        verify(exactly = 1) { scheduler.schedule() }
        scope.cancel()
    }

    @Test
    fun `adoption is a no-op with nothing to adopt, and never runs for the guest bucket`() = runTest {
        TestScopeSetup("user-a")
        coEvery { dao.adoptGuestRows(any(), any(), any()) } returns 0
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        val repo = UserSupplementRepository(dao, remote, session, scheduler, logger, scope)

        assertEquals(0, repo.adoptGuestEntries("user-a"))
        verify(exactly = 0) { scheduler.schedule() }

        assertEquals(0, repo.adoptGuestEntries(SessionRepository.LOCAL_USER_ID))
        coVerify(exactly = 1) { dao.adoptGuestRows(any(), any(), any()) } // guest call never hit the DAO
        scope.cancel()
    }
}
