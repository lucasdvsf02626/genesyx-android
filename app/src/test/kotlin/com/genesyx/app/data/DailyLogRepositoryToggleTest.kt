package com.genesyx.app.data

import com.genesyx.app.core.log.Logger
import com.genesyx.app.core.result.DataResult
import com.genesyx.app.data.local.entity.LogSyncStatus
import com.genesyx.app.data.local.entity.toEntity
import com.genesyx.app.data.remote.DailyLogRemoteDataSource
import com.genesyx.app.data.sync.DailyLogSyncScheduler
import com.genesyx.app.domain.consent.HealthDataCollectionGate
import com.genesyx.app.domain.model.DailyLog
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.time.LocalDate

/**
 * The Nutrition tab's chip toggle: what it writes, what it pushes, and what it reports.
 *
 * The case that matters most is un-logging the *last* supplement of the day. iOS's preferences
 * store once skipped "empty" writes as a no-op, so a clear never reached the server and the stale
 * copy came straight back on the next pull (ANDROID_PARITY.md §5). Here an empty list is written
 * and pushed like any other value — an explicit clear.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DailyLogRepositoryToggleTest {

    private val today: LocalDate = LocalDate.of(2026, 8, 26)
    private val logger = mockk<Logger>(relaxed = true)

    private fun session(userId: String? = "user-a"): SessionRepository = mockk<SessionRepository>().also {
        every { it.userId } returns MutableStateFlow(userId)
        every { it.currentUserId() } returns (userId ?: SessionRepository.LOCAL_USER_ID)
    }

    private fun remote(online: Boolean): DailyLogRemoteDataSource = mockk<DailyLogRemoteDataSource>().also {
        coEvery { it.upsertLog(any(), any(), any()) } returns
            if (online) DataResult.Success(Unit) else DataResult.Error(IOException("offline"), "offline")
    }

    private class Harness(
        val dao: FakeDailyLogDao,
        val remote: DailyLogRemoteDataSource,
        val scheduler: DailyLogSyncScheduler,
        val repo: DailyLogRepository,
        val scope: CoroutineScope,
    )

    private fun kotlinx.coroutines.test.TestScope.harness(
        online: Boolean = true,
        userId: String? = "user-a",
        gate: HealthDataCollectionGate = HealthDataCollectionGate { true },
    ): Harness {
        val dao = FakeDailyLogDao()
        val remote = remote(online)
        val scheduler = mockk<DailyLogSyncScheduler>(relaxed = true)
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        val repo = DailyLogRepository(dao, remote, session(userId), scheduler, logger, scope, gate)
        return Harness(dao, remote, scheduler, repo, scope)
    }

    @Test
    fun `logging a supplement writes its wire name and pushes the row`() = runTest {
        val h = harness()
        val result = h.repo.toggleSupplement("Folic acid", today)

        assertEquals(LogWriteResult.Saved, result)
        val row = h.dao.row("user-a", today)!!
        assertEquals(listOf("Folic acid"), row.supplements)
        assertEquals("confirmed by the server → SYNCED", LogSyncStatus.SYNCED, row.syncStatus)
        val pushed = slot<DailyLog>()
        coVerify(exactly = 1) { h.remote.upsertLog("user-a", today, capture(pushed)) }
        assertEquals(setOf("Folic acid"), pushed.captured.supplements)
        h.scope.cancel()
    }

    @Test
    fun `toggling twice un-logs it, and toggling off the only item persists an empty list — not a skipped write`() = runTest {
        val h = harness()
        h.repo.toggleSupplement("Folic acid", today)
        val result = h.repo.toggleSupplement("Folic acid", today)

        assertEquals(LogWriteResult.Saved, result)
        val row = h.dao.row("user-a", today)!!
        assertEquals("the row is written back with an EMPTY list", emptyList<String>(), row.supplements)
        assertEquals(2, h.dao.writes.size)

        // And the empty list reached the server — the explicit clear iOS had to learn to send.
        val pushes = mutableListOf<DailyLog>()
        coVerify(exactly = 2) { h.remote.upsertLog("user-a", today, capture(pushes)) }
        assertEquals(emptySet<String>(), pushes.last().supplements)
        h.scope.cancel()
    }

    @Test
    fun `un-logging removes every stored spelling of the same supplement`() = runTest {
        val h = harness()
        h.dao.upsert(DailyLog(supplements = setOf(" folic ACID ", "Folic acid", "Zinc")).let {
            it.toEntity("user-a", today)
        })
        h.repo.toggleSupplement("Folic acid", today)
        assertEquals(listOf("Zinc"), h.dao.row("user-a", today)!!.supplements)
        h.scope.cancel()
    }

    @Test
    fun `a toggle leaves everything else on the row alone`() = runTest {
        val h = harness()
        h.dao.upsert(DailyLog(waterMl = 1200, foodGroups = setOf("fruit"), notes = "ok").toEntity("user-a", today))
        h.repo.toggleSupplement("Zinc", today)
        val row = h.dao.row("user-a", today)!!
        assertEquals(1200, row.waterMl)
        assertEquals(listOf("fruit"), row.foodGroups)
        assertEquals("ok", row.notes)
        assertEquals(listOf("Zinc"), row.supplements)
        h.scope.cancel()
    }

    @Test
    fun `offline, the row is written and queued and the caller is told so — never told it saved`() = runTest {
        val h = harness(online = false)
        val result = h.repo.toggleSupplement("Zinc", today)

        assertEquals(LogWriteResult.Queued, result)
        val row = h.dao.row("user-a", today)!!
        assertEquals(listOf("Zinc"), row.supplements)
        assertEquals(LogSyncStatus.PENDING_UPSERT, row.syncStatus)
        verify(exactly = 1) { h.scheduler.schedule() }
        h.scope.cancel()
    }

    @Test
    fun `with consent withdrawn nothing is written and the refusal is reported`() = runTest {
        val h = harness(gate = HealthDataCollectionGate { false })
        val result = h.repo.toggleSupplement("Zinc", today)

        assertEquals(LogWriteResult.Refused, result)
        assertTrue(h.dao.writes.isEmpty())
        coVerify(exactly = 0) { h.remote.upsertLog(any(), any(), any()) }
        h.scope.cancel()
    }

    @Test
    fun `a guest's toggle stays on the device and reports saved`() = runTest {
        val h = harness(userId = null)
        val result = h.repo.toggleSupplement("Zinc", today)

        assertEquals(LogWriteResult.Saved, result)
        val row = h.dao.row(SessionRepository.LOCAL_USER_ID, today)!!
        assertEquals(listOf("Zinc"), row.supplements)
        assertEquals("no server target for a guest", LogSyncStatus.SYNCED, row.syncStatus)
        coVerify(exactly = 0) { h.remote.upsertLog(any(), any(), any()) }
        h.scope.cancel()
    }

    @Test
    fun `the toggled row is what logByDate emits — the chip fills from storage, not a guess`() = runTest {
        val h = harness()
        h.repo.toggleSupplement("Omega-3", today)
        assertEquals(setOf("Omega-3"), h.repo.logByDate.value[today]?.supplements)
        h.repo.toggleSupplement("Omega-3", today)
        assertEquals(emptySet<String>(), h.repo.logByDate.value[today]?.supplements)
        h.scope.cancel()
    }

    // ── Guest adoption (audit P1 #7): logs written before sign-in must follow her in. ──────────

    @Test
    fun `guest daily logs are adopted onto the account, marked pending, and queued for push`() = runTest {
        val h = harness()
        h.dao.upsert(DailyLog(waterMl = 500).toEntity(SessionRepository.LOCAL_USER_ID, today))

        val adopted = h.repo.adoptGuestLogs("user-a")

        assertEquals(1, adopted)
        val row = h.dao.row("user-a", today)!!
        assertEquals(500, row.waterMl)
        assertEquals("adopted rows ride the ordinary queue", LogSyncStatus.PENDING_UPSERT, row.syncStatus)
        verify { h.scheduler.schedule() }
        h.scope.cancel()
    }

    @Test
    fun `a date the account already holds locally is not collided with`() = runTest {
        val h = harness()
        h.dao.upsert(DailyLog(waterMl = 999).toEntity("user-a", today))
        h.dao.upsert(DailyLog(waterMl = 500).toEntity(SessionRepository.LOCAL_USER_ID, today))

        val adopted = h.repo.adoptGuestLogs("user-a")

        assertEquals(0, adopted)
        assertEquals(999, h.dao.row("user-a", today)!!.waterMl)
        h.scope.cancel()
    }

    @Test
    fun `guest adoption is refused while consent is withdrawn`() = runTest {
        // Adoption marks rows PENDING_UPSERT — an upload in slow motion — so the gate applies.
        val h = harness(gate = HealthDataCollectionGate { false })
        h.dao.upsert(DailyLog(waterMl = 500).toEntity(SessionRepository.LOCAL_USER_ID, today))

        assertEquals(0, h.repo.adoptGuestLogs("user-a"))
        assertEquals(null, h.dao.row("user-a", today))
        h.scope.cancel()
    }
}
