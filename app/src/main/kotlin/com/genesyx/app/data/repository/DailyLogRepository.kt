package com.genesyx.app.data.repository

import com.genesyx.app.data.local.dao.DailyLogDao
import com.genesyx.app.data.local.entity.DailyLogEntity
import com.genesyx.app.data.remote.DailyLogDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

interface DailyLogRepository {
    fun getDailyLog(userId: String, date: LocalDate): Flow<DailyLogEntity?>
    fun getRecentLogs(userId: String, limit: Int = 30): Flow<List<DailyLogEntity>>
    suspend fun saveLog(entity: DailyLogEntity)
    suspend fun getStreakDays(userId: String): Int
}

@Singleton
class SupabaseDailyLogRepository @Inject constructor(
    private val dao: DailyLogDao,
    private val supabase: SupabaseClient,
) : DailyLogRepository {

    override fun getDailyLog(userId: String, date: LocalDate): Flow<DailyLogEntity?> =
        dao.getByUserAndDate(userId, date)

    override fun getRecentLogs(userId: String, limit: Int): Flow<List<DailyLogEntity>> =
        dao.getRecentByUser(userId, limit)

    override suspend fun saveLog(entity: DailyLogEntity) {
        dao.upsert(entity)
        runCatching {
            supabase.postgrest["daily_logs"].upsert(
                DailyLogDto(
                    id = entity.id,
                    userId = entity.userId,
                    date = entity.date.toString(),
                    mood = entity.mood,
                    energy = entity.energy,
                    symptoms = entity.symptoms,
                    sleepMinutes = entity.sleepMinutes,
                    waterMl = entity.waterMl,
                    supplements = entity.supplements,
                    notes = entity.notes,
                ),
                onConflict = "user_id,date",
            )
        }
    }

    override suspend fun getStreakDays(userId: String): Int {
        var streak = 0
        var date = LocalDate.now()
        while (dao.getStreakCount(userId, date) > 0) {
            streak++
            date = date.minusDays(1)
            if (streak > 365) break
        }
        return streak
    }
}
