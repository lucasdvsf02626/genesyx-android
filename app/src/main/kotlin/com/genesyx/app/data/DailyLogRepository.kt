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
import com.genesyx.app.domain.consent.HealthDataCollectionGate
import com.genesyx.app.domain.model.DailyLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    // Hilt always supplies the real gate (BindingsModule binds ConsentRepository). The permissive
    // default exists so a test can construct this without standing up Room, mirroring iOS.
    private val consent: HealthDataCollectionGate = HealthDataCollectionGate { true },
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
            if (!consent.isCollectionPermitted()) {
                logger.w("DailyLog", "upsert refused — health-data consent withdrawn")
                return@launch
            }
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

    /**
     * Push one log; on success mark SYNCED, on failure leave it PENDING and enqueue a retry.
     * Returns true when the server confirmed the write.
     */
    private suspend fun pushOrQueue(userId: String, date: LocalDate, log: DailyLog): Boolean =
        if (remote.upsertLog(userId, date, log) is DataResult.Error) {
            logger.w("DailyLog", "daily log $date push failed — queued for retry")
            scheduler.schedule()
            false
        } else {
            dao.setStatus(userId, date, LogSyncStatus.SYNCED)
            logger.i("DailyLog", "synced daily log $date")
            true
        }

    /** Drains all PENDING rows (called by [com.genesyx.app.data.sync.DailyLogSyncWorker]). */
    suspend fun syncPending(): Boolean {
        // A queue drained after a withdrawal uploads the very logs she just stopped us collecting.
        // Report success so WorkManager stops asking rather than retrying forever — same gate as
        // the pH and supplement drains (this one was missing it).
        if (!consent.isCollectionPermitted()) {
            logger.w("DailyLog", "sync refused — health-data consent withdrawn")
            return true
        }
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
    fun adjustWater(deltaMl: Int, date: LocalDate = LocalDate.now()) =
        mutateRow(date) { it.copy(waterMl = (it.waterMl + deltaMl).coerceIn(0, 10_000)) }

    /** Set today's hydration to [ml], clamped to 0..10000. */
    fun setWater(ml: Int, date: LocalDate = LocalDate.now()) =
        mutateRow(date) { it.copy(waterMl = ml.coerceIn(0, 10_000)) }

    /** Set the night's sleep, clamped to 0..14h. Goes through [mutateRow] so it can't clobber water. */
    fun setSleep(minutes: Int, date: LocalDate = LocalDate.now()) =
        mutateRow(date) { it.copy(sleepMinutes = minutes.coerceIn(0, 14 * 60)) }

    /**
     * Save the Log Today form. The form owns mood/energy/symptoms/sleep/supplements/notes, but the
     * hydration total is owned by the quick-add trackers and may have moved while the form sat open —
     * so the stored row's water wins over the form's snapshot, never the other way round. This is
     * what stopped "My logs says 1.8 L while the tracker says 0.5 L".
     *
     * Food groups are held the same way for the same reason. Android gained its own editor on
     * 17 Aug 2026 (`edd8f2d`), but it did not move ownership here: the chips — on the Nutrition tab
     * and on this very form — write straight through [toggleFoodGroup], so this form's snapshot is
     * always the stale copy. Keep the term. (An earlier version of this comment said "only iOS can
     * log them" and told you to drop the term once Android had an editor; both halves are wrong now.)
     */
    fun upsertPreservingWater(date: LocalDate, log: DailyLog) =
        mutateRow(date) { stored -> log.copy(waterMl = stored.waterMl, foodGroups = stored.foodGroups) }

    /**
     * Add or remove one food group for a day. The Nutrition chips own this field, so the Log
     * form still goes through [upsertPreservingWater] and cannot wipe a tap made here.
     */
    fun toggleFoodGroup(id: String, date: LocalDate = LocalDate.now()) =
        mutateRow(date) { current ->
            val next = if (id in current.foodGroups) current.foodGroups - id else current.foodGroups + id
            current.copy(foodGroups = next)
        }

    /**
     * Add every id in [ids] without removing anything already recorded. Recipes use this so
     * "Log vegetables, protein…" never un-ticks a group she already had.
     */
    fun logFoodGroups(ids: Set<String>, date: LocalDate = LocalDate.now()) =
        mutateRow(date) { current -> current.copy(foodGroups = current.foodGroups + ids) }

    /**
     * Add or remove one supplement for a day — the Nutrition tab's tap-to-toggle chips.
     *
     * Unlike the fire-and-forget mutators above this one *reports*: a chip that appears to save
     * and then quietly reverts is the exact bug the profile screens had. The row is written to
     * Room first (so the chip fills before the network is consulted — the optimistic half), then
     * pushed; a failed push is queued and reported as [LogWriteResult.Queued], a withdrawn consent
     * as [LogWriteResult.Refused], and a local write that throws as [LogWriteResult.Failed].
     *
     * Un-logging the last supplement of the day writes the row back with an **empty** list, and
     * pushes that empty list — an explicit clear, never a skipped write. iOS learned this the hard
     * way (ANDROID_PARITY.md §5): a client that skips "empty" writes lets the server's stale copy
     * come straight back on the next pull.
     *
     * The work runs in the application scope, so a screen leaving mid-toggle cannot cancel it and
     * strand a PENDING row with no retry scheduled; the caller only awaits the outcome.
     */
    suspend fun toggleSupplement(stored: String, date: LocalDate = LocalDate.now()): LogWriteResult =
        scope.async {
            if (!consent.isCollectionPermitted()) {
                logger.w("DailyLog", "supplement toggle refused — health-data consent withdrawn")
                return@async LogWriteResult.Refused
            }
            val userId = session.currentUserId()
            val signedIn = userId != SessionRepository.LOCAL_USER_ID
            val status = if (signedIn) LogSyncStatus.PENDING_UPSERT else LogSyncStatus.SYNCED
            val next = try {
                writeMutex.withLock {
                    val current = dao.getByDate(userId, date)?.toDomain() ?: DailyLog()
                    // Matching is trimmed and case-insensitive, like every reader of this column;
                    // un-logging removes every stored spelling of the same supplement.
                    val matches = current.supplements.filter { it.trim().equals(stored.trim(), ignoreCase = true) }
                    val supplements =
                        if (matches.isNotEmpty()) current.supplements - matches.toSet()
                        else current.supplements + stored.trim()
                    current.copy(supplements = supplements).also { dao.upsert(it.toEntity(userId, date, status)) }
                }
            } catch (e: Exception) {
                logger.e("DailyLog", "supplement toggle failed to write locally", e)
                return@async LogWriteResult.Failed(e.message)
            }
            if (!signedIn) return@async LogWriteResult.Saved
            if (pushOrQueue(userId, date, next)) LogWriteResult.Saved else LogWriteResult.Queued
        }.await()

    private val writeMutex = Mutex()

    /**
     * Serialized read-modify-write against the stored row, not the in-memory snapshot. Two quick
     * +200 taps used to read the same pre-write value out of [logByDate] and the second silently
     * overwrote the first; reading the DAO under a mutex makes each tap see the previous one.
     * The network push stays outside the lock so an offline retry can't stall the next tap.
     */
    private fun mutateRow(date: LocalDate, transform: (DailyLog) -> DailyLog) {
        scope.launch {
            if (!consent.isCollectionPermitted()) {
                logger.w("DailyLog", "edit refused — health-data consent withdrawn")
                return@launch
            }
            val userId = session.currentUserId()
            val signedIn = userId != SessionRepository.LOCAL_USER_ID
            val status = if (signedIn) LogSyncStatus.PENDING_UPSERT else LogSyncStatus.SYNCED
            val next = writeMutex.withLock {
                val current = dao.getByDate(userId, date)?.toDomain() ?: DailyLog()
                transform(current).also { dao.upsert(it.toEntity(userId, date, status)) }
            }
            if (signedIn) pushOrQueue(userId, date, next)
        }
    }

    /**
     * Adopts the guest bucket's logs into [userId]'s account on sign-in, marked PENDING_UPSERT so
     * the ordinary queue pushes them (same dance as pH readings and supplements). Runs BEFORE
     * [refresh]: adopted rows are unsynced, and refresh's merge never overwrites unsynced rows, so
     * they survive the pull and its trailing syncPending() pushes them. Without this, logs written
     * before signing in vanished from view the moment a session existed and were wiped by the next
     * sign-out's clearAllTables. Returns the count.
     */
    suspend fun adoptGuestLogs(userId: String): Int {
        if (userId == SessionRepository.LOCAL_USER_ID) return 0
        // Adoption marks rows PENDING_UPSERT, so it is an upload in slow motion — gate it.
        if (!consent.isCollectionPermitted()) {
            logger.w("DailyLog", "guest adoption refused — health-data consent withdrawn")
            return 0
        }
        val adopted = dao.adoptGuestRows(SessionRepository.LOCAL_USER_ID, userId)
        if (adopted > 0) {
            logger.i("DailyLog", "adopted $adopted guest daily log(s) into account")
            scheduler.schedule() // push survives even if the sign-in refresh fails
        }
        return adopted
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
        if (!consent.isCollectionPermitted()) {
            logger.w("DailyLog", "refresh refused — health-data consent withdrawn")
            return
        }
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
                    logger.i("DailyLog", "cached ${result.data.size} daily logs ($kept local edits kept)")
                }
                syncPending()
            }
            is DataResult.Error -> logger.w("DailyLog", "refresh failed: ${result.message}")
            DataResult.Loading -> Unit
        }
    }
}

/** Outcome of a reporting daily-log write — see [DailyLogRepository.toggleSupplement]. */
sealed interface LogWriteResult {
    /** Written locally and confirmed by the server (guests: on-device is the only store). */
    data object Saved : LogWriteResult

    /** Written locally; the push failed and a retry is queued. Honest "saved on this device". */
    data object Queued : LogWriteResult

    /** Nothing was written: health-data consent is withdrawn. */
    data object Refused : LogWriteResult

    /** Nothing was written: the local write itself threw. */
    data class Failed(val message: String?) : LogWriteResult
}
