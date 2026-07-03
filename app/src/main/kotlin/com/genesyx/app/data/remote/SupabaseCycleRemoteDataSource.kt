package com.genesyx.app.data.remote

import com.genesyx.app.core.log.Logger
import com.genesyx.app.core.result.DataResult
import com.genesyx.app.data.remote.dto.CycleSettingsDto
import com.genesyx.app.domain.model.CycleSettings
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real Supabase (`cycle_settings` table) implementation. RLS scopes rows to `auth.uid()` on
 * `user_id`; upsert conflicts on the unique `user_id`. Bound only when creds are configured.
 */
@Singleton
class SupabaseCycleRemoteDataSource @Inject constructor(
    private val client: SupabaseClient,
    private val logger: Logger,
) : CycleRemoteDataSource {

    override suspend fun getCycleSettings(userId: String): DataResult<CycleSettings?> =
        try {
            val dto = client.from("cycle_settings")
                .select { filter { eq("user_id", userId) } }
                .decodeSingleOrNull<CycleSettingsDto>()
            DataResult.Success(
                dto?.let {
                    CycleSettings(
                        lastPeriodDate = LocalDate.parse(it.lastPeriodDate),
                        cycleLength = it.cycleLength,
                        periodLength = it.periodLength,
                    )
                },
            )
        } catch (t: Throwable) {
            logger.e("Cycle", "getCycleSettings failed", t)
            DataResult.Error(t, t.message)
        }

    override suspend fun upsertCycleSettings(userId: String, settings: CycleSettings): DataResult<Unit> =
        try {
            client.from("cycle_settings").upsert(
                CycleSettingsDto(
                    userId = userId,
                    cycleLength = settings.cycleLength,
                    periodLength = settings.periodLength,
                    lastPeriodDate = settings.lastPeriodDate.toString(),
                ),
            ) { onConflict = "user_id" }
            DataResult.Success(Unit)
        } catch (t: Throwable) {
            logger.e("Cycle", "upsertCycleSettings failed", t)
            DataResult.Error(t, t.message)
        }
}
