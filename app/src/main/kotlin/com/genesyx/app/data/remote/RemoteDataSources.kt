package com.genesyx.app.data.remote

import com.genesyx.app.core.result.DataResult
import com.genesyx.app.domain.model.Client
import com.genesyx.app.domain.model.CycleSettings
import com.genesyx.app.domain.model.DailyLog
import com.genesyx.app.domain.model.PhReading
import java.time.LocalDate

/**
 * Supabase-ready remote seams. Each mirrors a server-function contract in docs/DATA_LAYER.md and is
 * scoped by userId/ownerUserId (Supabase RLS enforces the same isolation server-side). Fallible ops
 * return [DataResult]. Local-first stubs (StubRemoteDataSources) satisfy these until creds are wired.
 */

interface CycleRemoteDataSource {
    suspend fun getCycleSettings(userId: String): DataResult<CycleSettings?>
    suspend fun upsertCycleSettings(userId: String, settings: CycleSettings): DataResult<Unit>
}

interface DailyLogRemoteDataSource {
    suspend fun listLogs(userId: String): DataResult<Map<LocalDate, DailyLog>>
    suspend fun getLog(userId: String, date: LocalDate): DataResult<DailyLog?>
    suspend fun upsertLog(userId: String, date: LocalDate, log: DailyLog): DataResult<Unit>
}

interface PhRemoteDataSource {
    suspend fun list(userId: String, sinceDays: Int?): DataResult<List<PhReading>>
    suspend fun upsert(userId: String, reading: PhReading): DataResult<Unit>
    suspend fun delete(userId: String, id: String): DataResult<Unit>
}

/** Minimal projection of the Supabase `profiles` row. */
data class RemoteProfile(
    val displayName: String?,
    val avatarUrl: String?,
    val partnerId: String?,
    val theme: String,
)

interface ProfileRemoteDataSource {
    suspend fun getProfile(userId: String): DataResult<RemoteProfile?>
    suspend fun upsertProfile(userId: String, profile: RemoteProfile): DataResult<Unit>
    suspend fun updateDisplayName(userId: String, name: String): DataResult<Unit>
    suspend fun updateTheme(userId: String, theme: String): DataResult<Unit>
}

interface ClientRemoteDataSource {
    suspend fun list(ownerUserId: String): DataResult<List<Client>>
    suspend fun upsert(client: Client): DataResult<Unit>
    suspend fun delete(ownerUserId: String, id: String): DataResult<Unit>
}
