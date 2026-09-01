package com.genesyx.app.data

import com.genesyx.app.core.log.Logger
import com.genesyx.app.core.result.DataResult
import com.genesyx.app.data.local.dao.PhReadingDao
import com.genesyx.app.data.remote.PhRemoteDataSource
import com.genesyx.app.data.sync.PhSyncScheduler
import com.genesyx.app.domain.model.PhMeasurement
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
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class PhRepositoryRangeTest {

    private val dao = mockk<PhReadingDao>(relaxed = true)
    private val remote = mockk<PhRemoteDataSource>(relaxed = true)
    private val scheduler = mockk<PhSyncScheduler>(relaxed = true)
    private val logger = mockk<Logger>(relaxed = true)
    private val session = mockk<SessionRepository>().apply {
        every { userId } returns MutableStateFlow<String?>("user-a")
        every { currentUserId() } returns "user-a"
        coEvery { awaitUserId() } returns "user-a"
    }

    private fun reading(ph: Double) =
        PhReading(id = "r1", phValue = ph, recordedAt = LocalDateTime.of(2026, 1, 1, 9, 0))

    /** Unconfined scope so the repo's fire-and-forget `scope.launch { dao.upsert(...) }` runs
     *  eagerly to first suspension (the relaxed dao completes immediately) — no advance needed. */
    private fun repo(scope: CoroutineScope): PhRepository {
        every { dao.observeAll(any()) } returns flowOf(emptyList())
        coEvery { remote.upsert(any()) } returns DataResult.Success(Unit)
        return PhRepository(dao, remote, session, scheduler, logger, scope)
    }

    // Boundaries are the client-approved vaginal range 3.8–7.0 (see PhStatus).

    @Test
    fun `lower boundary 3-8 is accepted and persisted`() = runTest {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        val result = repo(scope).create(reading(3.8))
        assertTrue(result is PhWriteResult.Accepted)
        coVerify(exactly = 1) { dao.upsert(match { it.phValue == 3.8 }) }
        scope.cancel()
    }

    @Test
    fun `upper boundary 7-0 is accepted and persisted`() = runTest {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        val result = repo(scope).create(reading(7.0))
        assertTrue(result is PhWriteResult.Accepted)
        coVerify(exactly = 1) { dao.upsert(match { it.phValue == 7.0 }) }
        scope.cancel()
    }

    @Test
    fun `below range 3-7 is rejected and never persisted`() = runTest {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        val result = repo(scope).create(reading(3.7))
        assertEquals(PhWriteResult.OutOfRange(3.7), result)
        coVerify(exactly = 0) { dao.upsert(any()) }
        scope.cancel()
    }

    @Test
    fun `above range 7-1 is rejected and never persisted`() = runTest {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        val result = repo(scope).create(reading(7.1))
        assertEquals(PhWriteResult.OutOfRange(7.1), result)
        coVerify(exactly = 0) { dao.upsert(any()) }
        scope.cancel()
    }

    @Test
    fun `a new reading defaults to a vaginal measurement`() = runTest {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        repo(scope).create(reading(4.2))
        coVerify(exactly = 1) { dao.upsert(match { it.measurementType == "vaginal" }) }
        scope.cancel()
    }

    // A legacy urine row predates the vaginal switch and sits on a different scale, so it is never
    // re-validated against the vaginal range — otherwise even a notes-only edit of 7.5 is refused.

    @Test
    fun `a legacy urine reading above the vaginal range is never re-validated on edit`() = runTest {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        val legacy = reading(7.5).copy(measurementType = PhMeasurement.URINE)
        val result = repo(scope).update(legacy)
        assertTrue(result is PhWriteResult.Accepted)
        coVerify(exactly = 1) {
            dao.upsert(match { it.phValue == 7.5 && it.measurementType == PhMeasurement.URINE })
        }
        scope.cancel()
    }

    @Test
    fun `a vaginal reading above the range is still rejected`() = runTest {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        val result = repo(scope).update(reading(7.5)) // vaginal by default
        assertEquals(PhWriteResult.OutOfRange(7.5), result)
        coVerify(exactly = 0) { dao.upsert(any()) }
        scope.cancel()
    }
}
