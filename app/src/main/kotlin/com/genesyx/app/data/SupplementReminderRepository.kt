package com.genesyx.app.data

import com.genesyx.app.core.di.ApplicationScope
import com.genesyx.app.data.local.datastore.GenesyxPreferencesDataStore
import com.genesyx.app.domain.model.Supplement
import com.genesyx.app.domain.model.UserSupplement
import com.genesyx.app.notifications.SupplementReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Customer-set per-supplement daily reminders. Device-local (a reminder schedule is a phone setting,
 * not shared health data), so nothing here touches Supabase or the cross-platform contract. Storage
 * is DataStore (supplement id → minutes-of-day); delivery is [SupplementReminderScheduler].
 */
@Singleton
class SupplementReminderRepository @Inject constructor(
    private val store: GenesyxPreferencesDataStore,
    private val scheduler: SupplementReminderScheduler,
    @ApplicationScope private val scope: CoroutineScope,
) {
    /** supplement id → reminder time in minutes-of-day. Absent id = no reminder. */
    val reminders: StateFlow<Map<String, Int>> =
        store.supplementReminders.stateIn(scope, SharingStarted.Eagerly, emptyMap())

    fun setReminder(id: String, name: String, minutesOfDay: Int) {
        scope.launch {
            store.setSupplementReminder(id, minutesOfDay)
            scheduler.schedule(id, name, minutesOfDay)
        }
    }

    fun clearReminder(id: String) {
        scope.launch {
            store.removeSupplementReminder(id)
            scheduler.cancel(id)
        }
    }

    /**
     * Re-arm the surviving reminders and drop any whose supplement is gone. Called on app start (the
     * self-rescheduling chains must be re-armed if the process was killed) and whenever the
     * supplement list changes (a deleted supplement must not keep reminding).
     */
    fun reconcile(current: List<UserSupplement>) {
        scope.launch {
            val byId = current.associateBy { it.id }
            store.supplementReminders.first().forEach { (id, minutes) ->
                val name = byId[id]?.name ?: planReminderName(id)
                if (name == null) {
                    store.removeSupplementReminder(id)
                    scheduler.cancel(id)
                } else {
                    scheduler.schedule(id, name, minutes)
                }
            }
        }
    }

    companion object {
        /**
         * Reminder ids for the four bundled essentials (the plan sheet's bell). They are not
         * `user_supplements` rows, so [reconcile] must not drop them as "deleted".
         */
        private const val PLAN_PREFIX = "plan:"

        fun planReminderId(supplement: Supplement): String = PLAN_PREFIX + supplement.id

        /** The display name behind a plan reminder id, or null if [id] is not one. */
        fun planReminderName(id: String): String? =
            id.removePrefix(PLAN_PREFIX).takeIf { it != id }
                ?.let { sid -> Supplement.entries.firstOrNull { it.id == sid }?.displayName }
    }

    /** Sign-out / account deletion: cancel and forget every reminder. */
    fun clearAll() {
        scope.launch { clearAllNow() }
    }

    /**
     * [clearAll] the caller can sequence against — the teardown must not report success while the
     * cancels are still in flight (process death in that window left chains alive).
     */
    suspend fun clearAllNow() {
        scheduler.cancelAll()
        store.clearSupplementReminders()
    }
}
