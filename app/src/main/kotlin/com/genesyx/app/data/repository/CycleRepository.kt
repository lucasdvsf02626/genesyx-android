package com.genesyx.app.data.repository

import com.genesyx.app.data.local.dao.CycleSettingsDao
import com.genesyx.app.data.local.entity.CycleSettingsEntity
import com.genesyx.app.data.remote.CycleSettingsDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

interface CycleRepository {
    fun getCycleSettings(userId: String): Flow<CycleSettingsEntity?>
    suspend fun saveCycleSettings(entity: CycleSettingsEntity)
}

@Singleton
class SupabaseCycleRepository @Inject constructor(
    private val dao: CycleSettingsDao,
    private val supabase: SupabaseClient,
) : CycleRepository {

    override fun getCycleSettings(userId: String): Flow<CycleSettingsEntity?> =
        dao.getByUserId(userId)

    override suspend fun saveCycleSettings(entity: CycleSettingsEntity) {
        dao.upsert(entity)
        runCatching {
            supabase.postgrest["cycle_settings"].upsert(
                CycleSettingsDto(
                    id = entity.id,
                    userId = entity.userId,
                    cycleLength = entity.cycleLength,
                    periodLength = entity.periodLength,
                    lastPeriodDate = entity.lastPeriodDate.toString(),
                ),
                onConflict = "user_id",
            )
        }
    }
}
