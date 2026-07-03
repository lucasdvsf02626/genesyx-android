package com.genesyx.app.data.remote

import com.genesyx.app.core.log.Logger
import com.genesyx.app.core.result.DataResult
import com.genesyx.app.data.remote.dto.DailyLogDto
import com.genesyx.app.domain.model.DailyLog
import com.genesyx.app.domain.model.EnergyLevel
import com.genesyx.app.domain.model.Mood
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real Supabase (`daily_logs`) implementation. RLS scopes rows to `auth.uid()` on `user_id`;
 * upsert conflicts on the unique `(user_id, date)`. Bound only when creds are configured.
 */
@Singleton
class SupabaseDailyLogRemoteDataSource @Inject constructor(
    private val client: SupabaseClient,
    private val logger: Logger,
) : DailyLogRemoteDataSource {

    override suspend fun listLogs(userId: String): DataResult<Map<LocalDate, DailyLog>> =
        try {
            val rows = client.from("daily_logs")
                .select { filter { eq("user_id", userId) } }
                .decodeList<DailyLogDto>()
            DataResult.Success(rows.associate { LocalDate.parse(it.date) to it.toDomain() })
        } catch (t: Throwable) {
            logger.e("DailyLog", "listLogs failed", t)
            DataResult.Error(t, t.message)
        }

    override suspend fun getLog(userId: String, date: LocalDate): DataResult<DailyLog?> =
        try {
            val dto = client.from("daily_logs")
                .select { filter { eq("user_id", userId); eq("date", date.toString()) } }
                .decodeSingleOrNull<DailyLogDto>()
            DataResult.Success(dto?.toDomain())
        } catch (t: Throwable) {
            logger.e("DailyLog", "getLog failed", t)
            DataResult.Error(t, t.message)
        }

    override suspend fun upsertLog(userId: String, date: LocalDate, log: DailyLog): DataResult<Unit> =
        try {
            client.from("daily_logs").upsert(log.toDto(userId, date)) { onConflict = "user_id,date" }
            DataResult.Success(Unit)
        } catch (t: Throwable) {
            logger.e("DailyLog", "upsertLog failed", t)
            DataResult.Error(t, t.message)
        }

    private fun DailyLogDto.toDomain(): DailyLog = DailyLog(
        mood = mood?.let { id -> Mood.entries.firstOrNull { it.id == id } },
        energy = energy?.let { id -> EnergyLevel.entries.firstOrNull { it.id == id } },
        symptoms = symptoms.toSet(),
        sleepMinutes = sleepMinutes,
        supplements = supplements.toSet(),
        notes = notes,
        waterMl = waterMl,
    )

    private fun DailyLog.toDto(userId: String, date: LocalDate) = DailyLogDto(
        userId = userId,
        date = date.toString(),
        mood = mood?.id,
        energy = energy?.id,
        symptoms = symptoms.toList(),
        sleepMinutes = sleepMinutes,
        waterMl = waterMl,
        supplements = supplements.toList(),
        notes = notes,
    )
}
