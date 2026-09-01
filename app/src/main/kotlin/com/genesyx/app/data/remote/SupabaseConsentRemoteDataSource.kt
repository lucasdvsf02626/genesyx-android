package com.genesyx.app.data.remote

import com.genesyx.app.core.log.Logger
import com.genesyx.app.core.result.DataResult
import com.genesyx.app.data.remote.dto.ConsentEventDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real Supabase (`consent_events`) implementation. RLS scopes rows to `auth.uid()` on `user_id`.
 * The trail is append-only on both sides — upsert conflicts on the primary key `id` and a replayed
 * push is a no-op, never a rewrite. Bound only when creds are configured.
 */
@Singleton
class SupabaseConsentRemoteDataSource @Inject constructor(
    private val client: SupabaseClient,
    private val logger: Logger,
) : ConsentRemoteDataSource {

    override suspend fun list(userId: String): DataResult<List<ConsentEventDto>> =
        try {
            val rows = client.from("consent_events")
                .select { filter { eq("user_id", userId) } }
                .decodeList<ConsentEventDto>()
            DataResult.Success(rows)
        } catch (t: Throwable) {
            logger.e("Consent", "list failed", t)
            DataResult.Error(t, t.message)
        }

    override suspend fun upsert(event: ConsentEventDto): DataResult<Unit> =
        try {
            client.from("consent_events").upsert(event) { onConflict = "id" }
            DataResult.Success(Unit)
        } catch (t: Throwable) {
            logger.e("Consent", "upsert failed", t)
            DataResult.Error(t, t.message)
        }
}
