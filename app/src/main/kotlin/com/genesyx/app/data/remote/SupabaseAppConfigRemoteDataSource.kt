package com.genesyx.app.data.remote

import com.genesyx.app.core.log.Logger
import com.genesyx.app.core.result.DataResult
import com.genesyx.app.data.remote.dto.AppConfigEntryDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real Supabase (`app_config`) implementation. Read with the anon key at startup — no session
 * required. The table may not exist yet: any failure (missing table, RLS denial, transport) comes
 * back as [DataResult.Error] and the update gate fails open. Bound only when creds are configured.
 */
@Singleton
class SupabaseAppConfigRemoteDataSource @Inject constructor(
    private val client: SupabaseClient,
    private val logger: Logger,
) : AppConfigRemoteDataSource {

    override suspend fun getValue(key: String): DataResult<String?> =
        try {
            val rows = client.from("app_config")
                .select { filter { eq("key", key) }; limit(1) }
                .decodeList<AppConfigEntryDto>()
            DataResult.Success(rows.firstOrNull()?.value)
        } catch (t: Throwable) {
            logger.e("AppConfig", "getValue failed", t)
            DataResult.Error(t, t.message)
        }
}
