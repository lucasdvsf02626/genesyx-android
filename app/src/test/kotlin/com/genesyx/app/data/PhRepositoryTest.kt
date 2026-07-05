package com.genesyx.app.data

import app.cash.turbine.test
import com.genesyx.app.core.log.Logger
import com.genesyx.app.core.result.DataResult
import com.genesyx.app.data.local.dao.PhReadingDao
import com.genesyx.app.data.local.entity.PhReadingEntity
import com.genesyx.app.data.local.entity.PhSyncStatus
import com.genesyx.app.data.remote.PhRemoteDataSource
import com.genesyx.app.data.remote.dto.PhReadingDto
import com.genesyx.app.data.sync.PhSyncScheduler
import com.genesyx.app.domain.model.PhReading
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
class PhRepositoryTest {

    private val dao = mockk<PhReadingDao>(relaxed = true)
    private val remote = mockk<PhRemoteDataSource>(relaxed = true)
    private val scheduler = mockk<PhSyncScheduler>(relaxed = true)
    private val logger = mockk<Logger>(relaxed = true)
    private val session = mockk<SessionRepository>()

    private fun entity(id: String, uid: String, ph: Double) =
        PhReadingEntity(id, uid, ph, LocalDateTime.of(2026, 1, 1, 9, 0), null)

    private fun reading(ph: Double) =
        PhReading(id = "r1", phValue = ph, recordedAt = LocalDateTime.now())

    @Test
    fun `online create writes PENDING then marks SYNCED, no retry queued`() = runTest {
        every { session.userId } returns MutableStateFlow<String?>("user-a")
        every { session.currentUserId() } returns "user-a"
        every { dao.observeAll(any()) } returns flowOf(emptyList())
        coEvery { remote.upsert(any()) } returns DataResult.Success(Unit)
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))

        PhRepository(dao, remote, session, scheduler, logger, scope).create(reading(6.5))

        coVerify { dao.upsert(match { it.id == "r1" && it.syncStatus == PhSyncStatus.PENDING_UPSERT }) }
        coVerify { dao.setStatus("r1", PhSyncStatus.SYNCED) }   // pushed through
        verify(exactly = 0) { scheduler.schedule() }
        scope.cancel()
    }

    @Test
    fun `offline create stays PENDING and enqueues a retry (never blocks)`() = runTest {
        every { session.userId } returns MutableStateFlow<String?>("user-a")
        every { session.currentUserId() } returns "user-a"
        every { dao.observeAll(any()) } returns flowOf(emptyList())
        coEvery { remote.upsert(any()) } returns DataResult.Error(RuntimeException("offline"))
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))

        val result = PhRepository(dao, remote, session, scheduler, logger, scope).create(reading(6.5))

        assertEquals(PhWriteResult.Accepted, result)           // write is accepted locally, not blocked
        coVerify { dao.upsert(match { it.syncStatus == PhSyncStatus.PENDING_UPSERT }) }
        verify(exactly = 1) { scheduler.schedule() }           // queued for background retry
        coVerify(exactly = 0) { dao.setStatus("r1", PhSyncStatus.SYNCED) }
        scope.cancel()
    }

    @Test
    fun `delete soft-deletes (tombstone) and pushes it`() = runTest {
        every { session.userId } returns MutableStateFlow<String?>("user-a")
        every { dao.observeAll(any()) } returns flowOf(emptyList())
        coEvery { dao.getById("r1") } returns
            entity("r1", "user-a", 6.5).copy(syncStatus = PhSyncStatus.PENDING_DELETE)
        coEvery { remote.upsert(any()) } returns DataResult.Success(Unit)
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))

        PhRepository(dao, remote, session, scheduler, logger, scope).delete("r1")

        coVerify { dao.markDeleted(eq("r1"), any()) }
        coVerify { remote.upsert(any()) }
        scope.cancel()
    }

    @Test
    fun `refresh merges server rows by id without duplicating`() = runTest {
        every { session.userId } returns MutableStateFlow<String?>("user-a")
        every { dao.observeAll(any()) } returns flowOf(emptyList())
        coEvery { dao.pending() } returns emptyList()
        coEvery { dao.getById("r1") } returns null   // not present locally yet
        coEvery { remote.list("user-a") } returns DataResult.Success(
            listOf(PhReadingDto("r1", "user-a", 6.5, "2026-01-01T09:00:00", updatedAt = "2026-01-01T09:00:00")),
        )
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))

        PhRepository(dao, remote, session, scheduler, logger, scope).refresh("user-a")

        coVerify(exactly = 1) { dao.upsert(match { it.id == "r1" }) }   // upsert-by-id: no duplicate
        scope.cancel()
    }

    @Test
    fun `readings re-scopes to the DAO query when the signed-in user changes`() = runTest {
        val userId = MutableStateFlow<String?>("user-a")
        every { session.userId } returns userId
        every { dao.observeAll("user-a") } returns flowOf(listOf(entity("a1", "user-a", 6.5)))
        every { dao.observeAll("user-b") } returns flowOf(listOf(entity("b1", "user-b", 7.0)))
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        val repo = PhRepository(dao, remote, session, scheduler, logger, scope)

        repo.readings.test {
            var first = awaitItem()
            if (first.isEmpty()) first = awaitItem()
            assertEquals(listOf("a1"), first.map { it.id })   // user-a's rows

            userId.value = "user-b"
            assertEquals(listOf("b1"), awaitItem().map { it.id })  // flatMapLatest re-queries
            cancelAndIgnoreRemainingEvents()
        }
        scope.cancel()
    }
}
