package com.genesyx.app.data

import com.genesyx.app.data.local.dao.DailyLogDao
import com.genesyx.app.data.local.dao.PhReadingDao
import com.genesyx.app.data.local.dao.UserSupplementDao
import com.genesyx.app.data.sync.DailyLogSyncScheduler
import com.genesyx.app.data.sync.PhSyncScheduler
import com.genesyx.app.data.sync.UserSupplementSyncScheduler
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

/**
 * The Profile sync-status row must tell the truth: the count is the sum of unsynced rows across
 * all three synced stores for the CURRENT user, and "Sync now" must kick every queue, not just one.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SyncStatusRepositoryTest {

    private val dailyLogDao = mockk<DailyLogDao>()
    private val phDao = mockk<PhReadingDao>()
    private val supplementDao = mockk<UserSupplementDao>()
    private val session = mockk<SessionRepository>()
    private val dailyLogScheduler = mockk<DailyLogSyncScheduler>(relaxed = true)
    private val phScheduler = mockk<PhSyncScheduler>(relaxed = true)
    private val supplementScheduler = mockk<UserSupplementSyncScheduler>(relaxed = true)

    private fun repo(scope: CoroutineScope) = SyncStatusRepository(
        dailyLogDao, phDao, supplementDao, session,
        dailyLogScheduler, phScheduler, supplementScheduler, scope,
    )

    @Test
    fun `pending count sums the three stores for the signed-in user`() = runTest {
        every { session.userId } returns MutableStateFlow<String?>("user-a")
        every { dailyLogDao.observePendingCount("user-a") } returns flowOf(2)
        every { phDao.observePendingCount("user-a") } returns flowOf(1)
        every { supplementDao.observePendingCount("user-a") } returns flowOf(3)
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))

        assertEquals(6, repo(scope).pendingCount.value)
        scope.cancel()
    }

    @Test
    fun `signed out falls back to the local user bucket`() = runTest {
        every { session.userId } returns MutableStateFlow<String?>(null)
        every { dailyLogDao.observePendingCount(SessionRepository.LOCAL_USER_ID) } returns flowOf(0)
        every { phDao.observePendingCount(SessionRepository.LOCAL_USER_ID) } returns flowOf(0)
        every { supplementDao.observePendingCount(SessionRepository.LOCAL_USER_ID) } returns flowOf(0)
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))

        assertEquals(0, repo(scope).pendingCount.value)
        scope.cancel()
    }

    @Test
    fun `sync now kicks every queue`() = runTest {
        every { session.userId } returns MutableStateFlow<String?>("user-a")
        every { dailyLogDao.observePendingCount(any()) } returns flowOf(0)
        every { phDao.observePendingCount(any()) } returns flowOf(0)
        every { supplementDao.observePendingCount(any()) } returns flowOf(0)
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))

        repo(scope).syncNow()

        verify(exactly = 1) { dailyLogScheduler.syncNow() }
        verify(exactly = 1) { phScheduler.syncNow() }
        verify(exactly = 1) { supplementScheduler.syncNow() }
        scope.cancel()
    }
}
