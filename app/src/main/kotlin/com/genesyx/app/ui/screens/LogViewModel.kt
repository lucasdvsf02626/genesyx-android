package com.genesyx.app.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.genesyx.app.data.DailyLogRepository
import com.genesyx.app.data.MealLogRepository
import com.genesyx.app.data.PreferencesRepository
import com.genesyx.app.data.UserSupplementRepository
import com.genesyx.app.domain.hydration.HydrationUnit
import com.genesyx.app.domain.model.DailyLog
import com.genesyx.app.domain.model.MealEntry
import com.genesyx.app.domain.model.Supplement
import com.genesyx.app.ui.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class LogViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val dailyLogRepository: DailyLogRepository,
    private val mealLogRepository: MealLogRepository,
    preferencesRepository: PreferencesRepository,
    userSupplementRepository: UserSupplementRepository,
) : ViewModel() {

    /**
     * The day this editor reads and writes. Today unless the route carried a valid, non-future
     * ISO date (the Track calendar's "edit this day"). Everything below keys off this — the form
     * seed, the water mini-card, and the save all target the same row.
     */
    val date: LocalDate = resolveDate(savedStateHandle.get<String>(Screen.Log.ARG_DATE), LocalDate.now())

    val isToday: Boolean = date == LocalDate.now()

    /**
     * The user's own supplements, as the names the Supplements dialog toggles and the log stores.
     * Entries whose name collides with a built-in row (wire or display name, case-insensitive) are
     * dropped — otherwise the dialog would show two rows toggling different strings for one thing.
     */
    val customSupplementNames: StateFlow<List<String>> = userSupplementRepository.supplements
        .map { entries ->
            val builtIn = Supplement.loggable
                .flatMap { listOf(it.wireName.lowercase(), it.displayName.lowercase()) }
                .toSet()
            entries.map { it.name }.distinct().filter { it.lowercase() !in builtIn }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Display unit for the Water mini-card. */
    val hydrationUnit: StateFlow<HydrationUnit> = preferencesRepository.hydrationUnit

    /** The form must not seed itself until this is true — see [DailyLogRepository.loaded]. */
    val loaded: StateFlow<Boolean> = dailyLogRepository.loaded

    fun initialLog(): DailyLog = dailyLogRepository.logOn(date)

    /**
     * Live hydration total for the Water mini-card. The form does NOT own this number — the quick-add
     * trackers do — so it renders the shared aggregate rather than a snapshot taken at open time.
     */
    val waterMl: StateFlow<Int> = dailyLogRepository.logByDate
        .map { it[date]?.waterMl ?: 0 }
        .stateIn(viewModelScope, SharingStarted.Eagerly, dailyLogRepository.waterMlOn(date))

    /** Writes through the same repository path as the quick-add trackers. */
    fun setWater(ml: Int) = dailyLogRepository.setWater(ml, date)

    /** Live food-group set for [date] — same row the Nutrition chips write. */
    val foodGroups: StateFlow<Set<String>> = dailyLogRepository.logByDate
        .map { it[date]?.foodGroups.orEmpty() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, dailyLogRepository.logOn(date).foodGroups)

    fun toggleFoodGroup(id: String) = dailyLogRepository.toggleFoodGroup(id, date)

    /** Free-text meals for [date] — same Room store the Nutrition tab uses. */
    val meals: StateFlow<List<MealEntry>> = mealLogRepository.mealsForDate(date)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun logMeal(entry: MealEntry): Boolean = mealLogRepository.log(entry.copy(date = date))

    fun deleteMeal(id: String) = mealLogRepository.delete(id)

    /**
     * Saves online or off. The repository writes to Room and queues the push if it fails, so there is
     * nothing to gain by checking connectivity first — v1.0's `isOnline()` gate existed only because
     * an offline write could be silently overwritten by the next read-through.
     *
     * Water is deliberately preserved from the stored row, not taken from [log] — see
     * [DailyLogRepository.upsertPreservingWater].
     */
    fun save(log: DailyLog) = dailyLogRepository.upsertPreservingWater(date, log)

    companion object {
        /**
         * A malformed or future date falls back to today: predictions aren't loggable, and a bad
         * deep link must not open an editor that silently writes to the wrong row.
         */
        fun resolveDate(raw: String?, today: LocalDate): LocalDate =
            raw?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                ?.takeIf { !it.isAfter(today) }
                ?: today
    }
}
