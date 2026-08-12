package com.genesyx.app.data

import com.genesyx.app.core.di.ApplicationScope
import com.genesyx.app.data.local.dao.DailyLogDao
import com.genesyx.app.data.local.dao.PhReadingDao
import com.genesyx.app.data.local.dao.UserSupplementDao
import com.genesyx.app.data.sync.DailyLogSyncScheduler
import com.genesyx.app.data.sync.PhSyncScheduler
import com.genesyx.app.data.sync.UserSupplementSyncScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The one honest answer to "did my data reach the server?" — a live count of rows across the three
 * synced stores that are written locally but not yet pushed. Until this existed, a stalled queue
 * was invisible: failures went to Logcat and the rows sat PENDING with no user-facing signal.
 *
 * Guests write rows as SYNCED (no server target under RLS), so the count is naturally 0 signed out.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class SyncStatusRepository @Inject constructor(
    dailyLogDao: DailyLogDao,
    phReadingDao: PhReadingDao,
    userSupplementDao: UserSupplementDao,
    session: SessionRepository,
    private val dailyLogScheduler: DailyLogSyncScheduler,
    private val phScheduler: PhSyncScheduler,
    private val supplementScheduler: UserSupplementSyncScheduler,
    @ApplicationScope scope: CoroutineScope,
) {
    /** Unsynced local changes for the current user, across daily logs, pH readings, supplements. */
    val pendingCount: StateFlow<Int> =
        session.userId
            .flatMapLatest { uid ->
                val id = uid ?: SessionRepository.LOCAL_USER_ID
                combine(
                    dailyLogDao.observePendingCount(id),
                    phReadingDao.observePendingCount(id),
                    userSupplementDao.observePendingCount(id),
                ) { logs, ph, supplements -> logs + ph + supplements }
            }
            .stateIn(scope, SharingStarted.Eagerly, 0)

    /** User-initiated "Sync now": REPLACE-enqueues each drain so stuck constraints re-evaluate. */
    fun syncNow() {
        dailyLogScheduler.syncNow()
        phScheduler.syncNow()
        supplementScheduler.syncNow()
    }

    /**
     * Foreground self-heal: cheap KEEP enqueues that revive any drain an OEM task-killer dropped
     * while the app was away. A no-op when nothing is pending — each worker exits on an empty queue.
     */
    fun scheduleDrain() {
        dailyLogScheduler.schedule()
        phScheduler.schedule()
        supplementScheduler.schedule()
    }
}
