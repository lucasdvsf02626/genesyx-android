package com.genesyx.app.data.remote

import com.genesyx.app.core.log.Logger
import com.genesyx.app.core.result.DataResult
import com.genesyx.app.data.remote.dto.ConsentEventDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.from
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real Supabase (`consent_events`) implementation. RLS scopes rows to `auth.uid()` on `user_id`.
 * The trail is append-only and the table has NO UPDATE policy, so pushes are plain INSERTs —
 * an upsert's conflict branch would be refused by RLS, not merged (same reasoning as iOS's
 * `append`). The device-minted id makes a replay collide instead of double-recording, and that
 * collision is reported as Success. Bound only when creds are configured.
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

    override suspend fun insert(event: ConsentEventDto): DataResult<Unit> =
        try {
            client.from("consent_events").insert(event)
            DataResult.Success(Unit)
        } catch (t: Throwable) {
            if (isDuplicateKey(t)) {
                // A retry of a push that already landed: the trail holds the row. Done.
                DataResult.Success(Unit)
            } else {
                logger.e("Consent", "insert failed", t)
                DataResult.Error(t, t.message)
            }
        }
}

/**
 * True when an insert was rejected because the row already exists: Postgres unique_violation
 * (23505) surfaced by PostgREST as HTTP 409. Top-level so it is unit-testable without a
 * SupabaseClient — see ConsentInsertReplayTest.
 */
internal fun isDuplicateKey(t: Throwable): Boolean {
    val rest = t as? RestException ?: return false
    return rest.statusCode == 409 || rest.error == "23505"
}
