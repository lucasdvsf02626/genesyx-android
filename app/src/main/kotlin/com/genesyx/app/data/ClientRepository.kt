package com.genesyx.app.data

import com.genesyx.app.core.log.Logger
import com.genesyx.app.core.result.DataResult
import com.genesyx.app.core.result.runCatchingResult
import com.genesyx.app.data.local.dao.ClientDao
import com.genesyx.app.data.local.entity.toDomain
import com.genesyx.app.data.local.entity.toEntity
import com.genesyx.app.data.remote.ClientRemoteDataSource
import com.genesyx.app.domain.model.Client
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local-first client storage — the multi-account scaling seam. Room is the source of truth; the
 * Supabase-ready [ClientRemoteDataSource] is best-effort push. Owner-scoped and paginated, so one
 * account can hold well past 100 client records. Not surfaced in the UI yet (data-layer scaffold);
 * ready to power a Clients screen or a coach/clinician mode later.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class ClientRepository @Inject constructor(
    private val dao: ClientDao,
    private val remote: ClientRemoteDataSource,
    private val session: SessionRepository,
    private val logger: Logger,
) {
    /** Live list of the current owner's clients (alphabetical). */
    val clients: Flow<List<Client>> =
        session.userId
            .flatMapLatest { uid -> dao.observeAll(uid ?: SessionRepository.LOCAL_USER_ID) }
            .map { list -> list.map { it.toDomain() } }

    /** Page through clients — scalable listing for large owners. */
    suspend fun page(limit: Int, offset: Int): List<Client> =
        dao.page(session.currentUserId(), limit, offset).map { it.toDomain() }

    suspend fun count(): Int = dao.count(session.currentUserId())

    suspend fun get(id: String): Client? = dao.byId(id)?.toDomain()

    /** Upsert a client locally (source of truth), then best-effort remote push. */
    suspend fun upsert(client: Client): DataResult<Unit> = runCatchingResult {
        val now = System.currentTimeMillis()
        val toSave = client.copy(
            ownerUserId = session.currentUserId(),
            createdAt = if (client.createdAt == 0L) now else client.createdAt,
            updatedAt = now,
        )
        dao.upsert(toSave.toEntity())
        if (remote.upsert(toSave) is DataResult.Error) {
            logger.w("ClientRepository", "remote upsert deferred (offline/unconfigured)")
        }
    }

    suspend fun delete(id: String) {
        dao.delete(id)
        remote.delete(session.currentUserId(), id)
    }
}
