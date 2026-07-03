package com.genesyx.app.data.remote

import com.genesyx.app.core.result.DataResult
import com.genesyx.app.domain.model.Client
import com.genesyx.app.domain.model.CycleSettings
import com.genesyx.app.domain.model.DailyLog
import com.genesyx.app.domain.model.PhReading
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/*
 * Local-first no-op remote sources. Reads return empty/null (nothing synced yet); writes succeed as
 * no-ops so on-device persistence remains the source of truth while offline / unconfigured. Replace
 * each with a Supabase-backed impl (supabase-kt Postgrest) and rebind in BindingsModule once
 * BuildConfig carries creds (AppConfig.hasSupabase).
 * TODO(supabase): implement Postgrest calls per docs/DATA_LAYER.md (RLS scopes by auth.uid()).
 */

@Singleton
class StubCycleRemoteDataSource @Inject constructor() : CycleRemoteDataSource {
    override suspend fun getCycleSettings(userId: String): DataResult<CycleSettings?> =
        DataResult.Success(null)

    override suspend fun upsertCycleSettings(userId: String, settings: CycleSettings): DataResult<Unit> =
        DataResult.Success(Unit)
}

@Singleton
class StubDailyLogRemoteDataSource @Inject constructor() : DailyLogRemoteDataSource {
    override suspend fun listLogs(userId: String): DataResult<Map<LocalDate, DailyLog>> =
        DataResult.Success(emptyMap())

    override suspend fun getLog(userId: String, date: LocalDate): DataResult<DailyLog?> =
        DataResult.Success(null)

    override suspend fun upsertLog(userId: String, date: LocalDate, log: DailyLog): DataResult<Unit> =
        DataResult.Success(Unit)
}

@Singleton
class StubPhRemoteDataSource @Inject constructor() : PhRemoteDataSource {
    override suspend fun list(userId: String, sinceDays: Int?): DataResult<List<PhReading>> =
        DataResult.Success(emptyList())

    override suspend fun upsert(userId: String, reading: PhReading): DataResult<Unit> =
        DataResult.Success(Unit)

    override suspend fun delete(userId: String, id: String): DataResult<Unit> =
        DataResult.Success(Unit)
}

@Singleton
class StubProfileRemoteDataSource @Inject constructor() : ProfileRemoteDataSource {
    override suspend fun getProfile(userId: String): DataResult<RemoteProfile?> =
        DataResult.Success(null)

    override suspend fun upsertProfile(userId: String, profile: RemoteProfile): DataResult<Unit> =
        DataResult.Success(Unit)

    override suspend fun updateDisplayName(userId: String, name: String): DataResult<Unit> =
        DataResult.Success(Unit)

    override suspend fun updateTheme(userId: String, theme: String): DataResult<Unit> =
        DataResult.Success(Unit)
}

@Singleton
class StubClientRemoteDataSource @Inject constructor() : ClientRemoteDataSource {
    override suspend fun list(ownerUserId: String): DataResult<List<Client>> =
        DataResult.Success(emptyList())

    override suspend fun upsert(client: Client): DataResult<Unit> =
        DataResult.Success(Unit)

    override suspend fun delete(ownerUserId: String, id: String): DataResult<Unit> =
        DataResult.Success(Unit)
}
