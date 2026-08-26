package com.genesyx.app.data

import com.genesyx.app.data.local.dao.DailyLogDao
import com.genesyx.app.data.local.entity.DailyLogEntity
import com.genesyx.app.data.local.entity.LogSyncStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/**
 * An in-memory `daily_logs` table for JVM tests: real upsert semantics on the composite key and a
 * live flow, so a repository under test emits exactly what it wrote. Room itself needs the
 * Android runtime — [com.genesyx.app.data.DailyLogRepositoryTest] (instrumented) covers that.
 */
class FakeDailyLogDao : DailyLogDao {
    private val rows = MutableStateFlow<Map<Pair<String, LocalDate>, DailyLogEntity>>(emptyMap())

    /** Every write, in order — lets a test assert what was persisted, not just what is visible. */
    val writes = mutableListOf<DailyLogEntity>()

    fun row(userId: String, date: LocalDate): DailyLogEntity? = rows.value[userId to date]

    override fun observeAll(userId: String): Flow<List<DailyLogEntity>> =
        rows.map { all -> all.values.filter { it.userId == userId }.sortedByDescending { it.date } }

    override suspend fun upsert(entity: DailyLogEntity) {
        writes += entity
        rows.value = rows.value + ((entity.userId to entity.date) to entity)
    }

    override suspend fun getByDate(userId: String, date: LocalDate): DailyLogEntity? = rows.value[userId to date]

    override suspend fun pending(): List<DailyLogEntity> =
        rows.value.values.filter { it.syncStatus != LogSyncStatus.SYNCED }

    override fun observePendingCount(userId: String): Flow<Int> =
        rows.map { all -> all.values.count { it.userId == userId && it.syncStatus != LogSyncStatus.SYNCED } }

    override suspend fun setStatus(userId: String, date: LocalDate, status: LogSyncStatus) {
        val key = userId to date
        rows.value[key]?.let { rows.value = rows.value + (key to it.copy(syncStatus = status)) }
    }
}
