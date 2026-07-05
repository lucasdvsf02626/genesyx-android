package com.genesyx.app.data

import com.genesyx.app.core.di.ApplicationScope
import com.genesyx.app.core.log.Logger
import com.genesyx.app.core.result.DataResult
import com.genesyx.app.data.local.dao.PhReadingDao
import com.genesyx.app.data.local.entity.PhReadingEntity
import com.genesyx.app.data.local.entity.PhSyncStatus
import com.genesyx.app.data.local.entity.toDomain
import com.genesyx.app.data.local.entity.toEntity
import com.genesyx.app.data.remote.PhRemoteDataSource
import com.genesyx.app.data.remote.dto.toDto
import com.genesyx.app.data.remote.dto.toEntity
import com.genesyx.app.data.sync.PhSyncScheduler
import com.genesyx.app.domain.model.PhReading
import com.genesyx.app.domain.ph.PhStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

/**
 * Urine-pH store — local-first (Room is the source of truth) with offline-first Supabase sync.
 *
 * Writes land in Room immediately (instant UI) as PENDING, then push to Supabase; on failure the row
 * stays PENDING and a WorkManager job ([PhSyncScheduler]) retries with backoff — offline writes QUEUE,
 * never block. Deletes are soft (deletedAt tombstone) so they sync safely. [refresh] pulls on sign-in
 * / manual refresh, merging by id (no duplicates) with last-write-wins on updatedAt, and never
 * clobbering locally-pending edits. pH values are rounded to 1 dp and range-checked (4.5–9.0).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class PhRepository @Inject constructor(
    private val dao: PhReadingDao,
    private val remote: PhRemoteDataSource,
    private val session: SessionRepository,
    private val scheduler: PhSyncScheduler,
    private val logger: Logger,
    @ApplicationScope private val scope: CoroutineScope,
) {
    val readings: StateFlow<List<PhReading>> =
        session.userId
            .flatMapLatest { uid -> dao.observeAll(uid ?: SessionRepository.LOCAL_USER_ID) }
            .map { list -> list.map { it.toDomain() } }
            .stateIn(scope, SharingStarted.Eagerly, emptyList())

    private fun Double.round1(): Double = (this * 10).roundToInt() / 10.0

    fun create(reading: PhReading): PhWriteResult = write(reading)

    fun update(reading: PhReading): PhWriteResult = write(reading)

    private fun write(reading: PhReading): PhWriteResult {
        // Enforce the trackable urine-pH range in the data layer (defense-in-depth beyond the UI
        // slider). Out-of-range values are rejected, never persisted. Boundaries are inclusive.
        val value = reading.phValue.round1()
        if (value < PhStatus.MIN || value > PhStatus.MAX) {
            logger.w("Ph", "rejected out-of-range pH $value (allowed ${PhStatus.MIN}..${PhStatus.MAX})")
            return PhWriteResult.OutOfRange(value)
        }
        scope.launch {
            val userId = session.currentUserId()
            val entity = reading.copy(phValue = value).toEntity(
                userId = userId,
                syncStatus = PhSyncStatus.PENDING_UPSERT,
                updatedAt = LocalDateTime.now(),
            )
            dao.upsert(entity)
            logger.i("Ph", "saved pH reading ${entity.id} locally for $userId")
            pushOrQueue(entity)
        }
        return PhWriteResult.Accepted
    }

    fun delete(id: String) {
        scope.launch {
            dao.markDeleted(id, LocalDateTime.now())
            val tombstone = dao.getById(id) ?: return@launch
            pushOrQueue(tombstone)
        }
    }

    /** Push one row; on success mark SYNCED, on failure keep it PENDING and enqueue a retry. */
    private suspend fun pushOrQueue(entity: PhReadingEntity) {
        when (remote.upsert(entity.toDto())) {
            is DataResult.Success -> dao.setStatus(entity.id, PhSyncStatus.SYNCED)
            is DataResult.Error -> {
                logger.w("Ph", "pH ${entity.id} push failed — queued for retry")
                scheduler.schedule()
            }
            DataResult.Loading -> Unit
        }
    }

    /** Drains all PENDING rows (called by [com.genesyx.app.data.sync.PhSyncWorker]). */
    suspend fun syncPending(): Boolean {
        var allSynced = true
        for (entity in dao.pending()) {
            if (remote.upsert(entity.toDto()) is DataResult.Success) {
                dao.setStatus(entity.id, PhSyncStatus.SYNCED)
            } else {
                allSynced = false
            }
        }
        return allSynced
    }

    /**
     * Read-through pull. Merges by id (upsert → no duplicates), last-write-wins on updatedAt, and
     * never overwrites a row with unsynced local changes. Then drains anything still PENDING.
     */
    suspend fun refresh(userId: String = session.currentUserId()) {
        when (val result = remote.list(userId)) {
            is DataResult.Success -> {
                for (dto in result.data) {
                    val local = dao.getById(dto.id)
                    if (local != null && local.syncStatus != PhSyncStatus.SYNCED) continue // keep local edits
                    val incoming = dto.toEntity(userId)
                    val localTs = local?.updatedAt
                    if (local == null || localTs == null || incoming.updatedAt == null ||
                        !incoming.updatedAt.isBefore(localTs)
                    ) {
                        dao.upsert(incoming)
                    }
                }
                syncPending()
            }
            is DataResult.Error ->
                logger.w("Ph", "pH pull failed — keeping local, will retry", result.throwable)
            DataResult.Loading -> Unit
        }
    }
}

/** Outcome of a pH write. Callers may surface [OutOfRange] to the user; the value is not persisted. */
sealed interface PhWriteResult {
    data object Accepted : PhWriteResult
    data class OutOfRange(
        val value: Double,
        val min: Double = PhStatus.MIN,
        val max: Double = PhStatus.MAX,
    ) : PhWriteResult
}
