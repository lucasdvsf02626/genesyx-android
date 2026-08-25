package com.genesyx.app.data

import com.genesyx.app.core.di.ApplicationScope
import com.genesyx.app.core.log.Logger
import com.genesyx.app.core.result.DataResult
import com.genesyx.app.core.result.SaveOutcome
import com.genesyx.app.data.local.dao.CycleSettingsDao
import com.genesyx.app.data.local.entity.toDomain
import com.genesyx.app.data.local.entity.toEntity
import com.genesyx.app.data.remote.CycleRemoteDataSource
import com.genesyx.app.domain.consent.HealthDataCollectionGate
import com.genesyx.app.domain.model.CycleSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cycle settings — local-first (Room source of truth) with Supabase sync. Public API is unchanged
 * (`settings` StateFlow + `upsert`) so Home / Track / Nutrition / Insights are untouched; `upsert`
 * now write-throughs to Supabase and `refresh` read-throughs on sign-in. Mirrors `cycle_settings`
 * (docs/DATA_LAYER.md), scoped per user.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class CycleRepository @Inject constructor(
    private val dao: CycleSettingsDao,
    private val remote: CycleRemoteDataSource,
    private val session: SessionRepository,
    private val logger: Logger,
    @ApplicationScope private val scope: CoroutineScope,
    // Hilt always supplies the real gate (BindingsModule binds ConsentRepository). The permissive
    // default exists so a test can construct this without standing up Room, mirroring iOS.
    private val consent: HealthDataCollectionGate = HealthDataCollectionGate { true },
) {
    val settings: StateFlow<CycleSettings?> =
        session.userId
            .flatMapLatest { uid -> dao.observe(uid ?: SessionRepository.LOCAL_USER_ID) }
            .map { it?.toDomain() }
            .stateIn(scope, SharingStarted.Eagerly, null)

    /**
     * Write-through: save locally (source of truth) then push to Supabase.
     *
     * Reports rather than launching blind: the caller pressed Save and is owed an answer. A refusal
     * that only reached the log is what made the editors look like they saved and then reverted.
     * Note [SaveOutcome.Failed] still means the local write landed — cycle settings have no retry
     * queue, so "saved here, not synced" is the honest reading.
     *
     * The work runs on [scope] and is only *awaited* by the caller. Callers are ViewModels, so their
     * scope dies when she backs out of the screen — and with no retry queue behind it, a write
     * cancelled mid-flight is simply gone. This way her cycle dates land whether or not she waits.
     */
    suspend fun upsert(settings: CycleSettings): SaveOutcome = scope.async {
        if (!consent.isCollectionPermitted()) {
            logger.w("Cycle", "upsert refused — health-data consent withdrawn")
            return@async SaveOutcome.Refused
        }
        val userId = session.awaitUserId()
        dao.upsert(settings.toEntity(userId))
        when (val result = remote.upsertCycleSettings(userId, settings)) {
            is DataResult.Error -> {
                logger.w("Cycle", "remote upsert deferred (offline/unconfigured)")
                SaveOutcome.Failed(result.message)
            }
            else -> {
                logger.i("Cycle", "synced cycle settings for $userId")
                SaveOutcome.Saved
            }
        }
    }.await()

    /**
     * Read-through: pull the remote row into the local cache (called after sign-in).
     *
     * Gated as tightly as the write. After a withdrawal there is no lawful basis to pull her health
     * data back down, and a pull is what makes a withdrawal look like it never happened.
     */
    suspend fun refresh(userId: String = session.currentUserId()) {
        if (!consent.isCollectionPermitted()) {
            logger.w("Cycle", "refresh refused — health-data consent withdrawn")
            return
        }
        when (val result = remote.getCycleSettings(userId)) {
            is DataResult.Success -> result.data?.let {
                dao.upsert(it.toEntity(userId))
                logger.i("Cycle", "cached cycle settings for $userId")
            }
            is DataResult.Error -> logger.w("Cycle", "refresh failed: ${result.message}")
            DataResult.Loading -> Unit
        }
    }
}
