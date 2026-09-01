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
import com.genesyx.app.domain.consent.HealthDataCollectionGate
import com.genesyx.app.domain.model.PhMeasurement
import com.genesyx.app.domain.model.PhReading
import com.genesyx.app.domain.ph.PhStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
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
 * Vaginal-pH store — local-first (Room is the source of truth) with offline-first Supabase sync.
 *
 * Writes land in Room immediately (instant UI) as PENDING, then push to Supabase; on failure the row
 * stays PENDING and a WorkManager job ([PhSyncScheduler]) retries with backoff — offline writes QUEUE,
 * never block. Deletes are soft (deletedAt tombstone) so they sync safely. [refresh] pulls on sign-in
 * / manual refresh, merging by id (no duplicates) with last-write-wins on updatedAt, and never
 * clobbering locally-pending edits. pH values are rounded to 1 dp and range-checked (3.8–7.0;
 * see PhStatus). New writes are vaginal; legacy urine rows are never re-validated.
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
    // Hilt always supplies the real gate (BindingsModule binds ConsentRepository). The permissive
    // default exists so a test can construct this without standing up Room, mirroring iOS.
    private val consent: HealthDataCollectionGate = HealthDataCollectionGate { true },
) {
    val readings: StateFlow<List<PhReading>> =
        session.userId
            .flatMapLatest { uid -> dao.observeAll(uid ?: SessionRepository.LOCAL_USER_ID) }
            .map { list -> list.map { it.toDomain() } }
            .stateIn(scope, SharingStarted.Eagerly, emptyList())

    private fun Double.round1(): Double = (this * 10).roundToInt() / 10.0

    suspend fun create(reading: PhReading): PhWriteResult = write(reading)

    suspend fun update(reading: PhReading): PhWriteResult = write(reading)

    /**
     * Suspends until the write is settled locally, so the returned result is the truth about
     * whether anything was recorded. It used to launch and report [PhWriteResult.Accepted]
     * immediately, which meant a reading the consent gate refused still closed the dialog on a
     * success it never had.
     *
     * Runs on [scope] and is merely awaited by the caller, so leaving the pH tab mid-save cannot
     * cancel the Room write and lose the reading before there is anything for the queue to retry.
     */
    private suspend fun write(reading: PhReading): PhWriteResult = scope.async {
        // Enforce the trackable vaginal-pH range in the data layer (defense-in-depth beyond the UI
        // slider). Out-of-range values are rejected, never persisted. Boundaries are inclusive.
        val value = reading.phValue.round1()
        // Only vaginal readings are range-checked. A legacy urine row predates the vaginal switch
        // and sits on a different scale, so re-validating it against the vaginal range would refuse
        // even a notes-only edit — legacy urine rows are never re-validated (see the class doc).
        // The rejected value itself is health data and is deliberately not logged.
        if (reading.measurementType != PhMeasurement.URINE && (value < PhStatus.MIN || value > PhStatus.MAX)) {
            logger.w("Ph", "rejected an out-of-range vaginal pH value (allowed ${PhStatus.MIN}..${PhStatus.MAX})")
            return@async PhWriteResult.OutOfRange(value)
        }
        if (!consent.isCollectionPermitted()) {
            logger.w("Ph", "write refused — health-data consent withdrawn")
            return@async PhWriteResult.Refused
        }
        val userId = session.awaitUserId()
        val signedIn = userId != SessionRepository.LOCAL_USER_ID
        val entity = reading.copy(phValue = value).toEntity(
            userId = userId,
            // Guests have no server target (RLS scopes to auth.uid()). Mark SYNCED (no pending
            // sync) and never push/queue — otherwise the write would enqueue doomed retries.
            syncStatus = if (signedIn) PhSyncStatus.PENDING_UPSERT else PhSyncStatus.SYNCED,
            updatedAt = LocalDateTime.now(),
        )
        dao.upsert(entity)
        logger.i("Ph", "saved pH reading ${entity.id} locally")
        // The push stays on the app scope: the row is already safe in Room, and the dialog closing
        // must not cancel the upload mid-flight and strand it without a queued retry.
        if (signedIn) scope.launch { pushOrQueue(entity) }
        PhWriteResult.Accepted
    }.await()

    fun delete(id: String) {
        scope.launch {
            dao.markDeleted(id, LocalDateTime.now())
            val tombstone = dao.getById(id) ?: return@launch
            if (session.currentUserId() != SessionRepository.LOCAL_USER_ID) {
                pushOrQueue(tombstone)
            } else {
                dao.setStatus(id, PhSyncStatus.SYNCED) // guest tombstone stays local-only
            }
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
        // A queue drained after a withdrawal uploads the very readings she just stopped us
        // collecting. Report success so WorkManager stops asking rather than retrying forever.
        if (!consent.isCollectionPermitted()) {
            logger.w("Ph", "sync refused — health-data consent withdrawn")
            return true
        }
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
     * Adopts the guest bucket's readings into [userId]'s account on sign-in/sign-up, marking them
     * PENDING_UPSERT so the ordinary queue pushes them. Without this, readings logged before signing
     * in silently vanish from view the moment a session exists (rows are keyed by user id). Ids are
     * locally-minted UUIDs, so adopted rows cannot collide with server rows. Returns the count.
     */
    suspend fun adoptGuestReadings(userId: String): Int {
        if (userId == SessionRepository.LOCAL_USER_ID) return 0
        // Adoption marks rows PENDING_UPSERT, so it is an upload in slow motion. Her readings stay
        // on the device either way — withdrawal stops collection, it does not erase.
        if (!consent.isCollectionPermitted()) {
            logger.w("Ph", "guest adoption refused — health-data consent withdrawn")
            return 0
        }
        val adopted = dao.adoptGuestRows(SessionRepository.LOCAL_USER_ID, userId, LocalDateTime.now())
        if (adopted > 0) {
            logger.i("Ph", "adopted $adopted guest pH reading(s) into account")
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
        if (!consent.isCollectionPermitted()) {
            logger.w("Ph", "refresh refused — health-data consent withdrawn")
            return
        }
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

/**
 * Outcome of a pH write. Nothing is persisted for either refusal, and both are the caller's to
 * surface — a dialog that closes on one of these has told her a reading was saved that wasn't.
 */
sealed interface PhWriteResult {
    data object Accepted : PhWriteResult

    /** The health-data consent gate refused. The remedy is the consent row in Profile. */
    data object Refused : PhWriteResult

    data class OutOfRange(
        val value: Double,
        val min: Double = PhStatus.MIN,
        val max: Double = PhStatus.MAX,
    ) : PhWriteResult
}
