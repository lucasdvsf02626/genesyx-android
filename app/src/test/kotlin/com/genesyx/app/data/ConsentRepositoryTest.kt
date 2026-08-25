package com.genesyx.app.data

import com.genesyx.app.core.log.Logger
import com.genesyx.app.data.local.dao.ConsentEventDao
import com.genesyx.app.data.local.entity.ConsentEventEntity
import com.genesyx.app.domain.consent.ConsentAction
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The consent trail's invariants. These are the compliance guarantees, not implementation detail:
 * the trail is append-only, withdrawal is not erasure, and an install that has never been asked is
 * permitted rather than silently switched off.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ConsentRepositoryTest {

    /** An in-memory stand-in for the Room table, ordered the same way the real queries are. */
    private class FakeConsentDao : ConsentEventDao {
        val rows = MutableStateFlow<List<ConsentEventEntity>>(emptyList())

        override suspend fun insert(event: ConsentEventEntity) {
            rows.value = rows.value + event
        }

        private fun trail(userId: String) =
            rows.value.filter { it.userId == userId }.sortedWith(compareBy({ it.recordedAt }, { it.id }))

        override fun observeLatest(userId: String): Flow<ConsentEventEntity?> =
            rows.map { trail(userId).lastOrNull() }

        override suspend fun latest(userId: String): ConsentEventEntity? = trail(userId).lastOrNull()

        override fun observeTrail(userId: String): Flow<List<ConsentEventEntity>> =
            rows.map { trail(userId) }
    }

    private val dao = FakeConsentDao()
    private val logger = mockk<Logger>(relaxed = true)

    private fun repo(scope: CoroutineScope): ConsentRepository {
        val session = mockk<SessionRepository>()
        every { session.userId } returns MutableStateFlow<String?>("user-a")
        every { session.currentUserId() } returns "user-a"
        coEvery { session.awaitUserId() } returns "user-a"
        return ConsentRepository(dao, session, logger, scope)
    }

    @Test
    fun `an install that has never been asked is permitted, not silently switched off`() = runTest {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))

        assertTrue(repo(scope).isCollectionPermitted())

        scope.cancel()
    }

    @Test
    fun `withdrawing refuses collection`() = runTest {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        val consent = repo(scope)

        consent.grant()
        assertTrue(consent.isCollectionPermitted())
        consent.withdraw()

        assertFalse(consent.isCollectionPermitted())
        scope.cancel()
    }

    @Test
    fun `granting again after a withdrawal turns collection back on`() = runTest {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        val consent = repo(scope)

        consent.grant()
        consent.withdraw()
        consent.grant()

        assertTrue(consent.isCollectionPermitted())
        scope.cancel()
    }

    /**
     * The trail is the evidence that a grant happened. If a withdrawal overwrote the grant instead of
     * being appended after it, there would be nothing left to demonstrate consent was ever given.
     */
    @Test
    fun `a withdrawal cannot erase the grant that preceded it`() = runTest {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        val consent = repo(scope)

        consent.grant()
        consent.withdraw()

        val actions = dao.rows.value.map { it.action }
        assertEquals(listOf(ConsentAction.GRANTED.wire, ConsentAction.WITHDRAWN.wire), actions)
        scope.cancel()
    }

    @Test
    fun `isActive tracks the newest event for the UI`() = runTest {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        val consent = repo(scope)

        assertTrue(consent.isActive.value) // never asked
        consent.withdraw()
        assertFalse(consent.isActive.value)
        consent.grant()
        assertTrue(consent.isActive.value)

        scope.cancel()
    }
}
