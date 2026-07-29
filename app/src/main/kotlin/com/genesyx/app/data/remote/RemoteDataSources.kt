package com.genesyx.app.data.remote

import com.genesyx.app.core.result.DataResult
import com.genesyx.app.data.remote.dto.GenesyxProductDto
import com.genesyx.app.data.remote.dto.PhReadingDto
import com.genesyx.app.data.remote.dto.UserSupplementDto
import com.genesyx.app.domain.model.Client
import com.genesyx.app.domain.model.CycleSettings
import com.genesyx.app.domain.model.DailyLog
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
    /** All rows for the user, INCLUDING soft-deleted tombstones (deleted_at set) so deletes sync. */
    suspend fun list(userId: String): DataResult<List<PhReadingDto>>

    /** Upsert (conflict on id). A soft delete is an upsert of the row with deleted_at set. */
    suspend fun upsert(reading: PhReadingDto): DataResult<Unit>
}

interface UserSupplementRemoteDataSource {
    /** All rows for the user, INCLUDING soft-deleted tombstones (deleted_at set) so deletes sync. */
    suspend fun list(userId: String): DataResult<List<UserSupplementDto>>

    /** Upsert (conflict on id). A soft delete is an upsert of the row with deleted_at set. */
    suspend fun upsert(entry: UserSupplementDto): DataResult<Unit>
}

interface GenesyxProductRemoteDataSource {
    /** Active catalogue rows. RLS grants SELECT to signed-in users only — guests get an error,
     *  which callers render the same as an empty catalogue ("coming soon"). */
    suspend fun listActive(): DataResult<List<GenesyxProductDto>>
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

interface WaitlistRemoteDataSource {
    /** Store a waitlist signup. Pre-auth (anon) — the table's RLS allows insert-only. */
    suspend fun join(email: String): DataResult<Unit>
}

interface ClientRemoteDataSource {
    suspend fun list(ownerUserId: String): DataResult<List<Client>>
    suspend fun upsert(client: Client): DataResult<Unit>
    suspend fun delete(ownerUserId: String, id: String): DataResult<Unit>
}
