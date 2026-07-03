package com.genesyx.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.genesyx.app.data.local.entity.ClientEntity
import kotlinx.coroutines.flow.Flow

/** Owner-scoped client access. Paginated [page] + [count] support scaling well past 100 records. */
@Dao
interface ClientDao {
    @Query("SELECT * FROM clients WHERE ownerUserId = :ownerUserId ORDER BY displayName COLLATE NOCASE ASC")
    fun observeAll(ownerUserId: String): Flow<List<ClientEntity>>

    @Query(
        "SELECT * FROM clients WHERE ownerUserId = :ownerUserId " +
            "ORDER BY displayName COLLATE NOCASE ASC LIMIT :limit OFFSET :offset",
    )
    suspend fun page(ownerUserId: String, limit: Int, offset: Int): List<ClientEntity>

    @Query("SELECT * FROM clients WHERE id = :id LIMIT 1")
    suspend fun byId(id: String): ClientEntity?

    @Query("SELECT COUNT(*) FROM clients WHERE ownerUserId = :ownerUserId")
    suspend fun count(ownerUserId: String): Int

    @Upsert
    suspend fun upsert(entity: ClientEntity)

    @Query("DELETE FROM clients WHERE id = :id")
    suspend fun delete(id: String)
}
