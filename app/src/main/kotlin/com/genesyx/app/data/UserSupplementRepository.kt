package com.genesyx.app.data

import com.genesyx.app.core.di.ApplicationScope
import com.genesyx.app.core.log.Logger
import com.genesyx.app.core.result.DataResult
import com.genesyx.app.data.local.dao.UserSupplementDao
import com.genesyx.app.data.local.entity.SupplementSyncStatus
import com.genesyx.app.data.local.entity.UserSupplementEntity
import com.genesyx.app.data.local.entity.toDomain
import com.genesyx.app.data.local.entity.toEntity
import com.genesyx.app.data.remote.UserSupplementRemoteDataSource
import com.genesyx.app.data.remote.dto.toDto
import com.genesyx.app.data.remote.dto.toEntity
import com.genesyx.app.data.sync.UserSupplementSyncScheduler
import com.genesyx.app.domain.model.UserSupplement
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

/**
 * User-supplement store ("Add your own supplement") — local-first (Room is the source of truth)
 * with offline-first Supabase sync, cloned from [PhRepository]'s proven shape.
 *
 * Writes land in Room immediately (instant UI) as PENDING, then push to Supabase; on failure the
 * row stays PENDING and a WorkManager job ([UserSupplementSyncScheduler]) retries with backoff.
 * Deletes are soft (deletedAt tombstone) so they sync safely. [refresh] pulls on sign-in, merging
 * by id with last-write-wins on updatedAt, never clobbering locally-pending edits. Names are
 * trimmed and length-checked here (server contract: 1..60 chars) — defense-in-depth beyond the UI.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class UserSupplementRepository @Inject constructor(
    private val dao: UserSupplementDao,
    private val remote: UserSupplementRemoteDataSource,
    private val session: SessionRepository,
    private val scheduler: UserSupplementSyncScheduler,
    private val logger: Logger,
    @ApplicationScope private val scope: CoroutineScope,
) {
    val supplements: StateFlow<List<UserSupplement>> =
        session.userId
            .flatMapLatest { uid -> dao.observeAll(uid ?: SessionRepository.LOCAL_USER_ID) }
            .map { list -> list.map { it.toDomain() } }
            .stateIn(scope, SharingStarted.Eagerly, emptyList())

    fun create(entry: UserSupplement): SupplementWriteResult = write(entry, isNew = true)

    fun update(entry: UserSupplement): SupplementWriteResult = write(entry, isNew = false)

    private fun write(entry: UserSupplement, isNew: Boolean): SupplementWriteResult {
        val name = entry.name.trim()
        if (name.isEmpty() || name.length > UserSupplement.NAME_MAX_LENGTH) {
            logger.w("UserSupp", "rejected supplement name of length ${name.length}")
            return SupplementWriteResult.InvalidName
        }
        val dose = entry.dose?.trim()?.takeIf { it.isNotEmpty() }
        scope.launch {
            val userId = session.currentUserId()
            val signedIn = userId != SessionRepository.LOCAL_USER_ID
            val now = LocalDateTime.now()
            // Edits keep the original creation time so the list order is stable.
            val createdAt = if (isNew) now else dao.getById(entry.id)?.createdAt ?: now
            val entity = entry.copy(name = name, dose = dose).toEntity(
                userId = userId,
                createdAt = createdAt,
                // Guests have no server target (RLS scopes to auth.uid()). Mark SYNCED (no pending
                // sync) and never push/queue — otherwise the write would enqueue doomed retries.
                syncStatus = if (signedIn) SupplementSyncStatus.PENDING_UPSERT else SupplementSyncStatus.SYNCED,
                updatedAt = now,
            )
            dao.upsert(entity)
            logger.i("UserSupp", "saved supplement ${entity.id} locally for $userId")
            if (signedIn) pushOrQueue(entity)
        }
        return SupplementWriteResult.Accepted
    }

    fun delete(id: String) {
        scope.launch {
            dao.markDeleted(id, LocalDateTime.now())
            val tombstone = dao.getById(id) ?: return@launch
            if (session.currentUserId() != SessionRepository.LOCAL_USER_ID) {
                pushOrQueue(tombstone)
            } else {
                dao.setStatus(id, SupplementSyncStatus.SYNCED) // guest tombstone stays local-only
            }
        }
    }

    /** Push one row; on success mark SYNCED, on failure keep it PENDING and enqueue a retry. */
    private suspend fun pushOrQueue(entity: UserSupplementEntity) {
        when (remote.upsert(entity.toDto())) {
            is DataResult.Success -> dao.setStatus(entity.id, SupplementSyncStatus.SYNCED)
            is DataResult.Error -> {
                logger.w("UserSupp", "supplement ${entity.id} push failed — queued for retry")
                scheduler.schedule()
            }
            DataResult.Loading -> Unit
        }
    }

    /** Drains all PENDING rows (called by [com.genesyx.app.data.sync.UserSupplementSyncWorker]). */
    suspend fun syncPending(): Boolean {
        var allSynced = true
        for (entity in dao.pending()) {
            if (remote.upsert(entity.toDto()) is DataResult.Success) {
                dao.setStatus(entity.id, SupplementSyncStatus.SYNCED)
            } else {
                allSynced = false
            }
        }
        return allSynced
    }

    /**
     * Adopts the guest bucket's entries into [userId]'s account on sign-in/sign-up, marking them
     * PENDING_UPSERT so the ordinary queue pushes them. Ids are locally-minted UUIDs, so adopted
     * rows cannot collide with server rows. Returns the count.
     */
    suspend fun adoptGuestEntries(userId: String): Int {
        if (userId == SessionRepository.LOCAL_USER_ID) return 0
        val adopted = dao.adoptGuestRows(SessionRepository.LOCAL_USER_ID, userId, LocalDateTime.now())
        if (adopted > 0) {
            logger.i("UserSupp", "adopted $adopted guest supplement(s) into account")
            scheduler.schedule() // push survives even if the sign-in refresh fails
        }
        return adopted
    }

    /**
     * Read-through pull. Merges by id (upsert → no duplicates), last-write-wins on updatedAt, and
     * never overwrites a row with unsynced local changes. Then drains anything still PENDING.
     */
    suspend fun refresh(userId: String = session.currentUserId()) {
        if (userId == SessionRepository.LOCAL_USER_ID) return // guest: nothing to pull/push
        when (val result = remote.list(userId)) {
            is DataResult.Success -> {
                for (dto in result.data) {
                    val local = dao.getById(dto.id)
                    if (local != null && local.syncStatus != SupplementSyncStatus.SYNCED) continue // keep local edits
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
                logger.w("UserSupp", "supplement pull failed — keeping local, will retry", result.throwable)
            DataResult.Loading -> Unit
        }
    }
}

/** Outcome of a supplement write. [InvalidName] means blank or over the 60-char server contract. */
sealed interface SupplementWriteResult {
    data object Accepted : SupplementWriteResult
    data object InvalidName : SupplementWriteResult
}
