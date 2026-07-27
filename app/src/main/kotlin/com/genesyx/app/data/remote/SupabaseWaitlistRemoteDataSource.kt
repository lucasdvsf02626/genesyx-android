package com.genesyx.app.data.remote

import com.genesyx.app.core.log.Logger
import com.genesyx.app.core.result.DataResult
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

/** Wire model for the Supabase `waitlist_emails` row. Server fills id/created_at defaults. */
@Serializable
private data class WaitlistEmailDto(
    @SerialName("email") val email: String,
)

/**
 * Real Supabase (`waitlist_emails`) implementation. Runs pre-auth: RLS on the table allows anon
 * INSERT only (no select/update/delete), so the anon key can add a row but never read the list.
 * Duplicate joins are swallowed server-side (`on conflict do nothing` via ignoreDuplicates) so
 * re-submitting the same address still reads as success.
 */
@Singleton
class SupabaseWaitlistRemoteDataSource @Inject constructor(
    private val client: SupabaseClient,
    private val logger: Logger,
) : WaitlistRemoteDataSource {

    override suspend fun join(email: String): DataResult<Unit> =
        try {
            client.from("waitlist_emails").upsert(WaitlistEmailDto(email = email.trim().lowercase())) {
                onConflict = "email"
                ignoreDuplicates = true
            }
            DataResult.Success(Unit)
        } catch (t: Throwable) {
            logger.e("Waitlist", "join failed", t)
            DataResult.Error(t, t.message)
        }
}
