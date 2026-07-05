package com.genesyx.app.data

import com.genesyx.app.core.log.Logger
import com.genesyx.app.data.local.dao.PhReadingDao
import com.genesyx.app.data.remote.PhRemoteDataSource
import com.genesyx.app.domain.model.PhReading
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
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class PhRepositoryRangeTest {

    private val dao = mockk<PhReadingDao>(relaxed = true)
    private val remote = mockk<PhRemoteDataSource>(relaxed = true)
    private val logger = mockk<Logger>(relaxed = true)
    private val session = mockk<SessionRepository>().apply {
        every { userId } returns MutableStateFlow<String?>("user-a")
        every { currentUserId() } returns "user-a"
    }

    private fun reading(ph: Double) =
        PhReading(id = "r1", phValue = ph, recordedAt = LocalDateTime.of(2026, 1, 1, 9, 0))

    /** Unconfined scope so the repo's fire-and-forget `scope.launch { dao.upsert(...) }` runs
     *  eagerly to first suspension (the relaxed dao completes immediately) — no advance needed. */
    private fun repo(scope: CoroutineScope): PhRepository {
        every { dao.observeAll(any()) } returns flowOf(emptyList())
        return PhRepository(dao, remote, session, logger, scope)
    }

    @Test
    fun `lower boundary 4-5 is accepted and persisted`() = runTest {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        val result = repo(scope).create(reading(4.5))
        assertTrue(result is PhWriteResult.Accepted)
        coVerify(exactly = 1) { dao.upsert(match { it.phValue == 4.5 }) }
        scope.cancel()
    }

    @Test
    fun `upper boundary 9-0 is accepted and persisted`() = runTest {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        val result = repo(scope).create(reading(9.0))
        assertTrue(result is PhWriteResult.Accepted)
        coVerify(exactly = 1) { dao.upsert(match { it.phValue == 9.0 }) }
        scope.cancel()
    }

    @Test
    fun `below range 4-4 is rejected and never persisted`() = runTest {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        val result = repo(scope).create(reading(4.4))
        assertEquals(PhWriteResult.OutOfRange(4.4), result)
        coVerify(exactly = 0) { dao.upsert(any()) }
        scope.cancel()
    }

    @Test
    fun `above range 9-1 is rejected and never persisted`() = runTest {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        val result = repo(scope).create(reading(9.1))
        assertEquals(PhWriteResult.OutOfRange(9.1), result)
        coVerify(exactly = 0) { dao.upsert(any()) }
        scope.cancel()
    }
}
