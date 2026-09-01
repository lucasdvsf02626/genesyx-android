package com.genesyx.app.data

import com.genesyx.app.core.log.Logger
import com.genesyx.app.core.result.DataResult
import com.genesyx.app.data.local.dao.ConsentEventDao
import com.genesyx.app.data.local.entity.ConsentEventEntity
import com.genesyx.app.data.remote.ConsentRemoteDataSource
import com.genesyx.app.data.remote.dto.ConsentEventDto
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

        override suspend fun insertAll(events: List<ConsentEventEntity>) {
            // IGNORE semantics: an id already present is never rewritten.
            val known = rows.value.map { it.id }.toSet()
            rows.value = rows.value + events.filter { it.id !in known }
        }

        /** rowid order = insertion order = list order in this fake. */
        private fun trail(userId: String) = rows.value.filter { it.userId == userId }

        override suspend fun latest(userId: String): ConsentEventEntity? = trail(userId).lastOrNull()

        override suspend fun trailByInsertion(userId: String): List<ConsentEventEntity> = trail(userId)

        override fun observeTrailByInsertion(userId: String): Flow<List<ConsentEventEntity>> =
            rows.map { trail(userId) }

        override fun observeTrail(userId: String): Flow<List<ConsentEventEntity>> =
            rows.map { trail(userId).sortedWith(compareBy({ it.recordedAt }, { it.id })) }
    }

    /** In-memory server trail. Append-only like the real table; upsert-by-id replays are no-ops. */
    private class FakeConsentRemote(
        initial: List<ConsentEventDto> = emptyList(),
        var failList: Boolean = false,
    ) : ConsentRemoteDataSource {
        val rows = initial.toMutableList()

        override suspend fun list(userId: String): DataResult<List<ConsentEventDto>> =
            if (failList) DataResult.Error(RuntimeException("offline"))
            else DataResult.Success(rows.filter { it.userId == userId })

        override suspend fun upsert(event: ConsentEventDto): DataResult<Unit> {
            if (rows.none { it.id == event.id }) rows.add(event)
            return DataResult.Success(Unit)
        }
    }

    private val dao = FakeConsentDao()
    private val logger = mockk<Logger>(relaxed = true)

    private fun repo(scope: CoroutineScope, remote: ConsentRemoteDataSource = FakeConsentRemote()): ConsentRepository {
        val session = mockk<SessionRepository>()
        every { session.userId } returns MutableStateFlow<String?>("user-a")
        every { session.currentUserId() } returns "user-a"
        coEvery { session.awaitUserId() } returns "user-a"
        return ConsentRepository(dao, remote, session, logger, scope)
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

    // ── Consent durability (audit P1 #8): the trail follows the account, not the install. ──

    @Test
    fun `a reinstall cannot reverse a withdrawal recorded on the server`() = runTest {
        // Local trail empty (fresh install); the account withdrew after an earlier grant.
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        val remote = FakeConsentRemote(
            listOf(
                ConsentEventDto("e1", "user-a", ConsentAction.GRANTED.wire, "2026-07-01T10:00:00"),
                ConsentEventDto("e2", "user-a", ConsentAction.WITHDRAWN.wire, "2026-08-01T09:30:00"),
            ),
        )
        val consent = repo(scope, remote)

        assertTrue(consent.refresh("user-a"))

        // The empty local trail used to read as permitted — the defect. The pull decides now.
        assertFalse(consent.isCollectionPermitted())
        assertFalse(consent.needsDecision.value) // there IS an answer; nothing to ask
        scope.cancel()
    }

    @Test
    fun `a local answer the server never saw is pushed on sign-in`() = runTest {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        val remote = FakeConsentRemote()
        val consent = repo(scope, remote)
        consent.withdraw() // recorded while the push was unreachable, say

        consent.refresh("user-a")

        assertEquals(
            listOf(ConsentAction.WITHDRAWN.wire),
            remote.rows.filter { it.userId == "user-a" }.map { it.action },
        )
        assertFalse(consent.isCollectionPermitted())
        scope.cancel()
    }

    @Test
    fun `no answer anywhere means ask, not assume`() = runTest {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        val consent = repo(scope, FakeConsentRemote())

        consent.refresh("user-a")

        assertTrue(consent.needsDecision.value)
        assertFalse(consent.isCollectionPermitted()) // undecided is not permitted
        // Answering ends the ask and takes effect immediately.
        consent.grant()
        assertFalse(consent.needsDecision.value)
        assertTrue(consent.isCollectionPermitted())
        scope.cancel()
    }

    @Test
    fun `a failed pull keeps the local answer and prompts no one`() = runTest {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        val consent = repo(scope, FakeConsentRemote(failList = true))

        assertFalse(consent.refresh("user-a"))

        assertFalse(consent.needsDecision.value)
        assertTrue(consent.isCollectionPermitted()) // never-asked default stands offline
        scope.cancel()
    }

    @Test
    fun `the newest event wins across the merge even when the server's is older`() = runTest {
        // She granted on this device today; the server holds an old withdrawal from another phone.
        // The merged trail must still read GRANTED — insertion order alone would get this wrong.
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        val remote = FakeConsentRemote(
            listOf(ConsentEventDto("old", "user-a", ConsentAction.WITHDRAWN.wire, "2026-01-01T08:00:00")),
        )
        val consent = repo(scope, remote)
        consent.grant() // local, recordedAt = now

        consent.refresh("user-a")

        assertTrue(consent.isCollectionPermitted())
        assertEquals(2, dao.rows.value.count { it.userId == "user-a" }) // both events kept — evidence
        scope.cancel()
    }
}
