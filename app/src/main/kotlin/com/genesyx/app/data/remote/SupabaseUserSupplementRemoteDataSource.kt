package com.genesyx.app.data.remote

import com.genesyx.app.core.log.Logger
import com.genesyx.app.core.result.DataResult
import com.genesyx.app.data.remote.dto.GenesyxProductDto
import com.genesyx.app.data.remote.dto.UserSupplementDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real Supabase (`user_supplements`) implementation. RLS scopes rows to `auth.uid()` on `user_id`;
 * upsert conflicts on the primary key `id` (app-generated UUID). A soft delete is an upsert of the
 * row with `deleted_at` set. `list` returns tombstones too so server-side deletes reach this device.
 * Bound only when creds are configured.
 */
@Singleton
class SupabaseUserSupplementRemoteDataSource @Inject constructor(
    private val client: SupabaseClient,
    private val logger: Logger,
) : UserSupplementRemoteDataSource {

    override suspend fun list(userId: String): DataResult<List<UserSupplementDto>> =
        try {
            val rows = client.from("user_supplements")
                .select { filter { eq("user_id", userId) } }
                .decodeList<UserSupplementDto>()
            DataResult.Success(rows)
        } catch (t: Throwable) {
            logger.e("UserSupp", "list failed", t)
            DataResult.Error(t, t.message)
        }

    override suspend fun upsert(entry: UserSupplementDto): DataResult<Unit> =
        try {
            client.from("user_supplements").upsert(entry) { onConflict = "id" }
            DataResult.Success(Unit)
        } catch (t: Throwable) {
            logger.e("UserSupp", "upsert failed", t)
            DataResult.Error(t, t.message)
        }
}

/**
 * Real Supabase (`genesyx_products`) implementation — a read-only shared catalogue. RLS grants
 * SELECT to signed-in users only; sorting is done client-side (sort_order, then name) so the read
 * stays a plain filtered select.
 */
@Singleton
class SupabaseGenesyxProductRemoteDataSource @Inject constructor(
    private val client: SupabaseClient,
    private val logger: Logger,
) : GenesyxProductRemoteDataSource {

    override suspend fun listActive(): DataResult<List<GenesyxProductDto>> =
        try {
            val rows = client.from("genesyx_products")
                .select { filter { eq("active", true) } }
                .decodeList<GenesyxProductDto>()
                .sortedWith(compareBy({ it.sortOrder }, { it.name }))
            DataResult.Success(rows)
        } catch (t: Throwable) {
            logger.e("GenesyxProducts", "list failed", t)
            DataResult.Error(t, t.message)
        }
}
