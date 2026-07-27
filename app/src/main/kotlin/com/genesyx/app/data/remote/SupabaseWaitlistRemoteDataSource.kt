package com.genesyx.app.data.remote

import com.genesyx.app.core.log.Logger
import com.genesyx.app.core.result.DataResult
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real Supabase implementation, via the `join_waitlist` SECURITY DEFINER RPC (execute granted to
 * anon/authenticated). The function — not the client — inserts into `waitlist_emails` with an
 * untargeted `on conflict do nothing`, because the table is deliberately unreadable to clients
 * (insert-only grants) and PostgREST's targeted-conflict upsert requires SELECT privilege. The RPC
 * keeps both properties: duplicate joins no-op silently, and the email list can never be read or
 * enumerated with the anon key. The function lowercases/trims server-side; the client mirrors it.
 */
@Singleton
class SupabaseWaitlistRemoteDataSource @Inject constructor(
    private val client: SupabaseClient,
    private val logger: Logger,
) : WaitlistRemoteDataSource {

    override suspend fun join(email: String): DataResult<Unit> =
        try {
            client.postgrest.rpc(
                "join_waitlist",
                buildJsonObject { put("p_email", email.trim().lowercase()) },
            )
            DataResult.Success(Unit)
        } catch (t: Throwable) {
            logger.e("Waitlist", "join failed", t)
            DataResult.Error(t, t.message)
        }
}
