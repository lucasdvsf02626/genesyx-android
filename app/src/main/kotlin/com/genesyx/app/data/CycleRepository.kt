package com.genesyx.app.data

import com.genesyx.app.core.di.ApplicationScope
import com.genesyx.app.core.log.Logger
import com.genesyx.app.core.result.DataResult
import com.genesyx.app.core.result.SaveOutcome
import com.genesyx.app.data.local.dao.CycleSettingsDao
import com.genesyx.app.data.local.datastore.GenesyxPreferencesDataStore
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
import kotlinx.coroutines.flow.first
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
    private val store: GenesyxPreferencesDataStore,
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
     * Note [SaveOutcome.Failed] still means the local write landed — and it now marks the row as
     * OWED (same contract as quiz answers / display name), so [refresh] pushes it before pulling
     * instead of silently overwriting an offline edit with the stale server copy.
     *
     * The work runs on [scope] and is only *awaited* by the caller. Callers are ViewModels, so their
     * scope dies when she backs out of the screen — this way her cycle dates land whether or not
     * she waits.
     */
    suspend fun upsert(settings: CycleSettings): SaveOutcome = scope.async {
        if (!consent.isCollectionPermitted()) {
            logger.w("Cycle", "upsert refused — health-data consent withdrawn")
            return@async SaveOutcome.Refused
        }
        val userId = session.awaitUserId()
        dao.upsert(settings.toEntity(userId))
        // Owed until the server confirms taking it — set BEFORE the push so a crash mid-push errs
        // towards a redundant (idempotent) re-push rather than a lost edit.
        store.setCycleSettingsOwed(true)
        when (val result = remote.upsertCycleSettings(userId, settings)) {
            is DataResult.Error -> {
                logger.w("Cycle", "remote upsert deferred (offline/unconfigured) — owed")
                SaveOutcome.Failed(result.message)
            }
            else -> {
                store.setCycleSettingsOwed(false)
                logger.i("Cycle", "synced cycle settings")
                SaveOutcome.Saved
            }
        }
    }.await()

    /**
     * Adopts the guest bucket's settings into [userId]'s account on sign-in, when that account has
     * no local row of its own. Without this, cycle dates configured before signing in vanished from
     * view the moment a session existed (the row is keyed by user id) and were wiped by the next
     * sign-out's clearAllTables. [refresh] decides who wins against the server: a server row is
     * adopted over this copy (same rule as quiz answers); with no server row, this copy is pushed.
     */
    suspend fun adoptGuestSettings(userId: String) {
        if (userId == SessionRepository.LOCAL_USER_ID) return
        if (!consent.isCollectionPermitted()) {
            logger.w("Cycle", "guest adoption refused — health-data consent withdrawn")
            return
        }
        if (dao.get(userId) != null) return
        val guest = dao.get(SessionRepository.LOCAL_USER_ID) ?: return
        dao.upsert(guest.copy(userId = userId))
        logger.i("Cycle", "adopted guest cycle settings into account")
    }

    /**
     * Read-through: pull the remote row into the local cache (called after sign-in).
     *
     * Push-before-pull: an OWED local edit (a save whose push failed) is sent first, and a failed
     * send aborts the pull — the local copy is the newer of the two, and pulling over it is exactly
     * the silent data loss this order exists to prevent. Gated as tightly as the write: after a
     * withdrawal there is no lawful basis to pull her health data back down.
     */
    suspend fun refresh(userId: String = session.currentUserId()) {
        if (!consent.isCollectionPermitted()) {
            logger.w("Cycle", "refresh refused — health-data consent withdrawn")
            return
        }
        val local = dao.get(userId)
        if (store.cycleSettingsOwed.first()) {
            if (local == null) {
                store.setCycleSettingsOwed(false) // nothing to owe — a sign-out cleared the row
            } else if (remote.upsertCycleSettings(userId, local.toDomain()) is DataResult.Error) {
                logger.w("Cycle", "owed settings push deferred — not pulling over the local copy")
                return
            } else {
                store.setCycleSettingsOwed(false)
            }
        }
        when (val result = remote.getCycleSettings(userId)) {
            is DataResult.Success -> {
                val server = result.data
                if (server != null) {
                    dao.upsert(server.toEntity(userId))
                    logger.i("Cycle", "cached cycle settings")
                } else if (local != null) {
                    // No server row: the adopted guest settings become the account's (quiz rule).
                    remote.upsertCycleSettings(userId, local.toDomain())
                }
            }
            is DataResult.Error -> logger.w("Cycle", "refresh failed: ${result.message}")
            DataResult.Loading -> Unit
        }
    }
}
