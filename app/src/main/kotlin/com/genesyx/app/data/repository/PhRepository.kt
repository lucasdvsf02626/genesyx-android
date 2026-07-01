package com.genesyx.app.data.repository

import com.genesyx.app.data.local.dao.PhReadingDao
import com.genesyx.app.data.local.entity.PhReadingEntity
import com.genesyx.app.data.remote.PhReadingDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

interface PhRepository {
    fun getRecentReadings(userId: String, limit: Int = 20): Flow<List<PhReadingEntity>>
    suspend fun saveReading(entity: PhReadingEntity)
}

@Singleton
class SupabasePhRepository @Inject constructor(
    private val dao: PhReadingDao,
    private val supabase: SupabaseClient,
) : PhRepository {

    override fun getRecentReadings(userId: String, limit: Int): Flow<List<PhReadingEntity>> =
        dao.getRecentByUser(userId, limit)

    override suspend fun saveReading(entity: PhReadingEntity) {
        dao.upsert(entity)
        runCatching {
            supabase.postgrest["ph_readings"].upsert(
                PhReadingDto(
                    id = entity.id,
                    userId = entity.userId,
                    phValue = entity.phValue,
                    recordedAt = entity.recordedAt.toString(),
                    notes = entity.notes,
                )
            )
        }
    }
}
