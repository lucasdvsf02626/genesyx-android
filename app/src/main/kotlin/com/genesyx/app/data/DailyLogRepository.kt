package com.genesyx.app.data

import com.genesyx.app.core.di.ApplicationScope
import com.genesyx.app.core.log.Logger
import com.genesyx.app.core.result.DataResult
import com.genesyx.app.data.local.dao.DailyLogDao
import com.genesyx.app.data.local.entity.LogSyncStatus
import com.genesyx.app.data.local.entity.toDomain
import com.genesyx.app.data.local.entity.toEntity
import com.genesyx.app.data.remote.DailyLogRemoteDataSource
import com.genesyx.app.data.sync.DailyLogSyncScheduler
import com.genesyx.app.domain.model.DailyLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Daily-log store — local-first (Room source of truth) with offline-first Supabase sync.
 *
 * Writes land in Room immediately as PENDING, then push; on failure the row stays PENDING and a
 * WorkManager job ([DailyLogSyncScheduler]) retries with backoff — offline writes QUEUE, never block.
 * [refresh] pulls on sign-in and never overwrites a row with unsynced local changes. Mirrors
 * `daily_logs` (UNIQUE(user_id, date)).
 *
 * Streaks are not computed here — [com.genesyx.app.data.StreakRepository] owns that, feeding
 * [com.genesyx.app.domain.streaks.StreakEngine] from these logs plus the pH readings.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class DailyLogRepository @Inject constructor(
    private val dao: DailyLogDao,
    private val remote: DailyLogRemoteDataSource,
    private val session: SessionRepository,
    private val scheduler: DailyLogSyncScheduler,
    private val logger: Logger,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val logsOrNull: StateFlow<Map<LocalDate, DailyLog>?> =
        session.userId
            .flatMapLatest { uid -> dao.observeAll(uid ?: SessionRepository.LOCAL_USER_ID) }
            .map { list -> list.associate { it.date to it.toDomain() } }
            .stateIn(scope, SharingStarted.Eagerly, null)

    val logByDate: StateFlow<Map<LocalDate, DailyLog>> =
        logsOrNull
            .map { it.orEmpty() }
            .stateIn(scope, SharingStarted.Eagerly, emptyMap())

    /**
     * False until Room's first emission lands. An editor must not seed a form from [logOn] before
     * this is true: pre-load, an existing log is indistinguishable from no log, so saving that form
     * would overwrite the real row with blanks.
     */
    val loaded: StateFlow<Boolean> =
        logsOrNull
            .map { it != null }
            .stateIn(scope, SharingStarted.Eagerly, false)

    fun logOn(date: LocalDate): DailyLog = logByDate.value[date] ?: DailyLog()

    fun waterMlOn(date: LocalDate): Int = logOn(date).waterMl

    /**
     * Write-through: save locally (source of truth) as PENDING, then push. On failure the row stays
     * PENDING and a WorkManager job retries with backoff — an offline write QUEUES, it never blocks
     * and it is never lost. This is what replaced v1.0's "you're offline, can't save" gate.
     */
    fun upsert(date: LocalDate, log: DailyLog) {
        scope.launch {
            val userId = session.currentUserId()
            val signedIn = userId != SessionRepository.LOCAL_USER_ID
            // Guests have no server target (RLS scopes to auth.uid()), so a queued push would retry
            // forever against nothing. Mark SYNCED and keep them purely local, as pH does.
            val status = if (signedIn) LogSyncStatus.PENDING_UPSERT else LogSyncStatus.SYNCED
            dao.upsert(log.toEntity(userId, date, status))
            if (!signedIn) return@launch
            pushOrQueue(userId, date, log)
        }
    }

    /** Push one log; on success mark SYNCED, on failure leave it PENDING and enqueue a retry. */
    private suspend fun pushOrQueue(userId: String, date: LocalDate, log: DailyLog) {
        if (remote.upsertLog(userId, date, log) is DataResult.Error) {
            logger.w("DailyLog", "daily log $date push failed — queued for retry")
            scheduler.schedule()
        } else {
            dao.setStatus(userId, date, LogSyncStatus.SYNCED)
            logger.i("DailyLog", "synced daily log $date for $userId")
        }
    }

    /** Drains all PENDING rows (called by [com.genesyx.app.data.sync.DailyLogSyncWorker]). */
    suspend fun syncPending(): Boolean {
        var allSynced = true
        for (entity in dao.pending()) {
            if (remote.upsertLog(entity.userId, entity.date, entity.toDomain()) is DataResult.Success) {
                dao.setStatus(entity.userId, entity.date, LogSyncStatus.SYNCED)
            } else {
                allSynced = false
            }
        }
        return allSynced
    }

    /** Adjust today's hydration by [deltaMl], clamped to 0..10000. */
    fun adjustWater(deltaMl: Int, date: LocalDate = LocalDate.now()) {
        val next = (waterMlOn(date) + deltaMl).coerceIn(0, 10_000)
        upsert(date, logOn(date).copy(waterMl = next))
    }

    /** Set today's hydration to [ml], clamped to 0..10000. */
    fun setWater(ml: Int, date: LocalDate = LocalDate.now()) {
        upsert(date, logOn(date).copy(waterMl = ml.coerceIn(0, 10_000)))
    }

    /**
     * Read-through: pull the user's logs into the local cache (called after sign-in).
     *
     * A row with unsynced local changes is SKIPPED, never overwritten. That single rule is what makes
     * offline writes safe: before the queue existed, this loop happily stamped the server's copy over
     * an offline edit, which is the data loss the old "you're offline" gate was there to prevent.
     * Anything still pending is then pushed, so local wins and the server catches up.
     */
    suspend fun refresh(userId: String = session.currentUserId()) {
        when (val result = remote.listLogs(userId)) {
            is DataResult.Success -> {
                var kept = 0
                for ((date, log) in result.data) {
                    val local = dao.getByDate(userId, date)
                    if (local != null && local.syncStatus != LogSyncStatus.SYNCED) {
                        kept++
                        continue
                    }
                    dao.upsert(log.toEntity(userId, date))
                }
                if (result.data.isNotEmpty()) {
                    logger.i("DailyLog", "cached ${result.data.size} daily logs for $userId ($kept local edits kept)")
                }
                syncPending()
            }
            is DataResult.Error -> logger.w("DailyLog", "refresh failed: ${result.message}")
            DataResult.Loading -> Unit
        }
    }
}
