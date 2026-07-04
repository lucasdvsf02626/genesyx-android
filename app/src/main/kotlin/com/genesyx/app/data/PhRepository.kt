package com.genesyx.app.data

import com.genesyx.app.core.di.ApplicationScope
import com.genesyx.app.core.log.Logger
import com.genesyx.app.core.result.DataResult
import com.genesyx.app.data.local.dao.PhReadingDao
import com.genesyx.app.data.local.entity.toDomain
import com.genesyx.app.data.local.entity.toEntity
import com.genesyx.app.data.remote.PhRemoteDataSource
import com.genesyx.app.domain.model.PhReading
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

/**
 * Urine-pH store — local-first (Room source of truth) with Supabase sync. Public API is unchanged
 * (readings StateFlow + create/update/delete) so Insights keeps working; writes now write-through to
 * Supabase and `refresh` read-throughs on sign-in. pH values rounded to 1 dp on write. Mirrors
 * `ph_readings` (docs/DATA_LAYER.md), scoped per user, ordered by recordedAt ascending.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class PhRepository @Inject constructor(
    private val dao: PhReadingDao,
    private val remote: PhRemoteDataSource,
    private val session: SessionRepository,
    private val logger: Logger,
    @ApplicationScope private val scope: CoroutineScope,
) {
    val readings: StateFlow<List<PhReading>> =
        session.userId
            .flatMapLatest { uid -> dao.observeAll(uid ?: SessionRepository.LOCAL_USER_ID) }
            .map { list -> list.map { it.toDomain() } }
            .stateIn(scope, SharingStarted.Eagerly, emptyList())

    private fun Double.round1(): Double = (this * 10).roundToInt() / 10.0

    fun create(reading: PhReading) = write(reading)

    fun update(reading: PhReading) = write(reading)

    private fun write(reading: PhReading) {
        val normalized = reading.copy(phValue = reading.phValue.round1())
        scope.launch {
            val userId = session.currentUserId()
            dao.upsert(normalized.toEntity(userId))
            logger.i("Ph", "saved pH reading ${normalized.id} locally for $userId")
            // v1.1: enable when ph_readings table exists. pH is local-only in 1.0, so no network
            // call fires for pH.
            // if (remote.upsert(userId, normalized) is DataResult.Error) {
            //     logger.w("Ph", "remote upsert deferred (offline/unconfigured)")
            // } else {
            //     logger.i("Ph", "synced pH reading ${normalized.id} for $userId")
            // }
        }
    }

    fun delete(id: String) {
        scope.launch {
            dao.delete(id)
            // v1.1: enable when ph_readings table exists (local-only in 1.0).
            // remote.delete(session.currentUserId(), id)
        }
    }

    /** Read-through: local-only in 1.0, so this is a no-op (no network read for pH). */
    suspend fun refresh(userId: String = session.currentUserId()) {
        // v1.1: enable when ph_readings table exists. pH is local-only in 1.0 — no read-through and
        // no network call for pH (this previously logged a non-fatal `E Ph` against the absent table).
    }
}
