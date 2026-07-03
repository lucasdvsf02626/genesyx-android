package com.genesyx.app.data.remote

import com.genesyx.app.core.log.Logger
import com.genesyx.app.core.result.DataResult
import com.genesyx.app.data.remote.dto.PhReadingDto
import com.genesyx.app.domain.model.PhReading
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import java.time.LocalDateTime
import java.time.OffsetDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real Supabase (`ph_readings`) implementation. RLS scopes rows to `auth.uid()` on `user_id`;
 * upsert conflicts on the primary key `id` (app-generated UUID). Bound only when creds configured.
 */
@Singleton
class SupabasePhRemoteDataSource @Inject constructor(
    private val client: SupabaseClient,
    private val logger: Logger,
) : PhRemoteDataSource {

    override suspend fun list(userId: String, sinceDays: Int?): DataResult<List<PhReading>> =
        try {
            val rows = client.from("ph_readings")
                .select { filter { eq("user_id", userId) } }
                .decodeList<PhReadingDto>()
            DataResult.Success(rows.map { it.toDomain() }.sortedBy { it.recordedAt })
        } catch (t: Throwable) {
            logger.e("Ph", "list failed", t)
            DataResult.Error(t, t.message)
        }

    override suspend fun upsert(userId: String, reading: PhReading): DataResult<Unit> =
        try {
            client.from("ph_readings").upsert(reading.toDto(userId)) { onConflict = "id" }
            DataResult.Success(Unit)
        } catch (t: Throwable) {
            logger.e("Ph", "upsert failed", t)
            DataResult.Error(t, t.message)
        }

    override suspend fun delete(userId: String, id: String): DataResult<Unit> =
        try {
            client.from("ph_readings").delete { filter { eq("id", id) } }
            DataResult.Success(Unit)
        } catch (t: Throwable) {
            logger.e("Ph", "delete failed", t)
            DataResult.Error(t, t.message)
        }

    private fun PhReadingDto.toDomain(): PhReading =
        PhReading(id = id, phValue = phValue, recordedAt = parseTs(recordedAt), notes = notes)

    private fun PhReading.toDto(userId: String) =
        PhReadingDto(id = id, userId = userId, phValue = phValue, recordedAt = recordedAt.toString(), notes = notes)

    /** Postgres returns timestamptz with an offset; the app models recordedAt as local wall-clock. */
    private fun parseTs(s: String): LocalDateTime =
        runCatching { OffsetDateTime.parse(s).toLocalDateTime() }.getOrElse { LocalDateTime.parse(s) }
}
