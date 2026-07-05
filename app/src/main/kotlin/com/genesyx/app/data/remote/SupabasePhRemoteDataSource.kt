package com.genesyx.app.data.remote

import com.genesyx.app.core.log.Logger
import com.genesyx.app.core.result.DataResult
import com.genesyx.app.data.remote.dto.PhReadingDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real Supabase (`ph_readings`) implementation. RLS scopes rows to `auth.uid()` on `user_id`;
 * upsert conflicts on the primary key `id` (app-generated UUID). A soft delete is an upsert of the
 * row with `deleted_at` set. `list` returns tombstones too so server-side deletes reach this device.
 * Bound only when creds are configured.
 */
@Singleton
class SupabasePhRemoteDataSource @Inject constructor(
    private val client: SupabaseClient,
    private val logger: Logger,
) : PhRemoteDataSource {

    override suspend fun list(userId: String): DataResult<List<PhReadingDto>> =
        try {
            val rows = client.from("ph_readings")
                .select { filter { eq("user_id", userId) } }
                .decodeList<PhReadingDto>()
            DataResult.Success(rows)
        } catch (t: Throwable) {
            logger.e("Ph", "list failed", t)
            DataResult.Error(t, t.message)
        }

    override suspend fun upsert(reading: PhReadingDto): DataResult<Unit> =
        try {
            client.from("ph_readings").upsert(reading) { onConflict = "id" }
            DataResult.Success(Unit)
        } catch (t: Throwable) {
            logger.e("Ph", "upsert failed", t)
            DataResult.Error(t, t.message)
        }
}
