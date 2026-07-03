package com.genesyx.app.data

import com.genesyx.app.core.di.ApplicationScope
import com.genesyx.app.core.log.Logger
import com.genesyx.app.core.result.DataResult
import com.genesyx.app.data.local.dao.CycleSettingsDao
import com.genesyx.app.data.local.entity.toDomain
import com.genesyx.app.data.local.entity.toEntity
import com.genesyx.app.data.remote.CycleRemoteDataSource
import com.genesyx.app.domain.model.CycleSettings
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
) {
    val settings: StateFlow<CycleSettings?> =
        session.userId
            .flatMapLatest { uid -> dao.observe(uid ?: SessionRepository.LOCAL_USER_ID) }
            .map { it?.toDomain() }
            .stateIn(scope, SharingStarted.Eagerly, null)

    /** Write-through: save locally (source of truth) then push to Supabase (best-effort). */
    fun upsert(settings: CycleSettings) {
        scope.launch {
            val userId = session.currentUserId()
            dao.upsert(settings.toEntity(userId))
            if (remote.upsertCycleSettings(userId, settings) is DataResult.Error) {
                logger.w("Cycle", "remote upsert deferred (offline/unconfigured)")
            } else {
                logger.i("Cycle", "synced cycle settings for $userId")
            }
        }
    }

    /** Read-through: pull the remote row into the local cache (called after sign-in). */
    suspend fun refresh(userId: String = session.currentUserId()) {
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
