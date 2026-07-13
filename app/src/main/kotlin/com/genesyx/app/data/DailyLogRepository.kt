package com.genesyx.app.data

import com.genesyx.app.core.di.ApplicationScope
import com.genesyx.app.core.log.Logger
import com.genesyx.app.core.result.DataResult
import com.genesyx.app.data.local.dao.DailyLogDao
import com.genesyx.app.data.local.entity.toDomain
import com.genesyx.app.data.local.entity.toEntity
import com.genesyx.app.data.remote.DailyLogRemoteDataSource
import com.genesyx.app.domain.model.DailyLog
import com.genesyx.app.domain.streaks.StreakEngine
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
 * Daily-log store — local-first (Room source of truth) with Supabase sync. Public API is unchanged
 * (StateFlow + logOn/waterMlOn/upsert/adjustWater/setWater/streak) so Home / Nutrition / Log are
 * untouched; `upsert` now write-throughs to Supabase and `refresh` read-throughs all logs on
 * sign-in. Mirrors `daily_logs` (UNIQUE(user_id, date)); streak counts consecutive days with water.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class DailyLogRepository @Inject constructor(
    private val dao: DailyLogDao,
    private val remote: DailyLogRemoteDataSource,
    private val session: SessionRepository,
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

    /** Write-through: save locally (source of truth) then push to Supabase (best-effort). */
    fun upsert(date: LocalDate, log: DailyLog) {
        scope.launch {
            val userId = session.currentUserId()
            dao.upsert(log.toEntity(userId, date))
            if (remote.upsertLog(userId, date, log) is DataResult.Error) {
                logger.w("DailyLog", "remote upsert deferred (offline/unconfigured)")
            } else {
                logger.i("DailyLog", "synced daily log $date for $userId")
            }
        }
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
     * Consecutive days back from [today] (inclusive) that have water logged. Delegates to
     * [StreakEngine] so the rule lives in exactly one place; [StreakRepository] exposes the rest of
     * the streak state (weekly, best, milestones).
     */
    fun streak(today: LocalDate = LocalDate.now()): Int =
        StreakEngine.compute(logByDate.value, phByDate = emptySet(), today = today).dailyHydration

    /** Read-through: pull all of the user's logs into the local cache (called after sign-in). */
    suspend fun refresh(userId: String = session.currentUserId()) {
        when (val result = remote.listLogs(userId)) {
            is DataResult.Success -> {
                result.data.forEach { (date, log) -> dao.upsert(log.toEntity(userId, date)) }
                if (result.data.isNotEmpty()) {
                    logger.i("DailyLog", "cached ${result.data.size} daily logs for $userId")
                }
            }
            is DataResult.Error -> logger.w("DailyLog", "refresh failed: ${result.message}")
            DataResult.Loading -> Unit
        }
    }
}
