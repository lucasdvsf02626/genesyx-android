package com.genesyx.app.data

import app.cash.turbine.test
import com.genesyx.app.core.log.Logger
import com.genesyx.app.data.local.dao.PhReadingDao
import com.genesyx.app.data.local.entity.PhReadingEntity
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
import org.junit.Test
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class PhRepositoryTest {

    private val dao = mockk<PhReadingDao>(relaxed = true)
    private val remote = mockk<PhRemoteDataSource>(relaxed = true)
    private val logger = mockk<Logger>(relaxed = true)
    private val session = mockk<SessionRepository>()

    private fun entity(id: String, uid: String, ph: Double) =
        PhReadingEntity(id, uid, ph, LocalDateTime.of(2026, 1, 1, 9, 0), null)

    @Test
    fun `create writes an in-range reading scoped to the current user`() = runTest {
        every { session.userId } returns MutableStateFlow<String?>("user-a")
        every { session.currentUserId() } returns "user-a"
        every { dao.observeAll(any()) } returns flowOf(emptyList())
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))

        PhRepository(dao, remote, session, logger, scope)
            .create(PhReading(id = "r1", phValue = 6.5, recordedAt = LocalDateTime.now()))

        coVerify(exactly = 1) { dao.upsert(match { it.id == "r1" && it.userId == "user-a" }) }
        scope.cancel()
    }

    @Test
    fun `delete removes the reading by id`() = runTest {
        every { session.userId } returns MutableStateFlow<String?>("user-a")
        every { dao.observeAll(any()) } returns flowOf(emptyList())
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))

        PhRepository(dao, remote, session, logger, scope).delete("r1")

        coVerify(exactly = 1) { dao.delete("r1") }
        scope.cancel()
    }

    @Test
    fun `readings re-scopes to the DAO query when the signed-in user changes`() = runTest {
        val userId = MutableStateFlow<String?>("user-a")
        every { session.userId } returns userId
        every { dao.observeAll("user-a") } returns flowOf(listOf(entity("a1", "user-a", 6.5)))
        every { dao.observeAll("user-b") } returns flowOf(listOf(entity("b1", "user-b", 7.0)))
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        val repo = PhRepository(dao, remote, session, logger, scope)

        repo.readings.test {
            // stateIn seeds emptyList before the DAO flow lands; skip it if present
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
