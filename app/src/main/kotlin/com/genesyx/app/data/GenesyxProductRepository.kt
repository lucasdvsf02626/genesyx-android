package com.genesyx.app.data

import com.genesyx.app.core.log.Logger
import com.genesyx.app.core.result.DataResult
import com.genesyx.app.data.remote.GenesyxProductRemoteDataSource
import com.genesyx.app.data.remote.dto.toDomain
import com.genesyx.app.domain.model.GenesyxProduct
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Read-only view of the shared `genesyx_products` catalogue. Not cached in Room: the catalogue is
 * cosmetic (a browse list, not user data), currently has zero SKUs, and the UI's empty state
 * ("coming soon") is also the correct offline/guest rendering — RLS only lets signed-in users read
 * it, so guests and fetch failures all collapse to the same empty list.
 */
@Singleton
class GenesyxProductRepository @Inject constructor(
    private val remote: GenesyxProductRemoteDataSource,
    private val session: SessionRepository,
    private val logger: Logger,
) {
    suspend fun fetchCatalogue(): List<GenesyxProduct> {
        if (session.currentUserId() == SessionRepository.LOCAL_USER_ID) return emptyList()
        return when (val result = remote.listActive()) {
            is DataResult.Success -> result.data.map { it.toDomain() }
            is DataResult.Error -> {
                logger.w("GenesyxProducts", "catalogue fetch failed — showing coming soon", result.throwable)
                emptyList()
            }
            DataResult.Loading -> emptyList()
        }
    }
}
