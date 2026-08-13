package com.genesyx.app.data

import com.genesyx.app.core.di.ApplicationScope
import com.genesyx.app.core.log.Logger
import com.genesyx.app.data.local.dao.MealEntryDao
import com.genesyx.app.data.local.entity.toDomain
import com.genesyx.app.data.local.entity.toEntity
import com.genesyx.app.domain.model.MealEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The meal log — **local-only** (Room is the sole home; there is no Supabase table, so nothing to
 * sync and no cross-platform contract). Rows are scoped by userId exactly like the synced stores, so
 * accounts stay isolated on a shared device; `database.clearAllTables()` on sign-out / account
 * deletion wipes them, so no separate teardown is needed.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class MealLogRepository @Inject constructor(
    private val dao: MealEntryDao,
    private val session: SessionRepository,
    private val logger: Logger,
    @ApplicationScope private val scope: CoroutineScope,
) {
    /** A day's meals for whoever is signed in (or the guest bucket), newest first. */
    fun mealsForDate(date: LocalDate): Flow<List<MealEntry>> =
        session.userId
            .flatMapLatest { uid -> dao.observeForDate(uid ?: SessionRepository.LOCAL_USER_ID, date) }
            .map { list -> list.map { it.toDomain() } }

    /** Log a meal. Blank description is rejected; a long one is capped. Returns whether it was saved. */
    fun log(entry: MealEntry): Boolean {
        val description = entry.description.trim().take(MealEntry.DESCRIPTION_MAX_LENGTH)
        if (description.isEmpty()) {
            logger.w("MealLog", "rejected a blank meal description")
            return false
        }
        scope.launch {
            val userId = session.currentUserId()
            dao.upsert(entry.copy(description = description).toEntity(userId))
            logger.i("MealLog", "logged meal ${entry.id} for $userId")
        }
        return true
    }

    fun delete(id: String) {
        scope.launch { dao.delete(id) }
    }
}
