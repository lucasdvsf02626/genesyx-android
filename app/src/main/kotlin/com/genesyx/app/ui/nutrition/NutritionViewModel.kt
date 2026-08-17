package com.genesyx.app.ui.nutrition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.genesyx.app.data.CycleRepository
import com.genesyx.app.data.DailyLogRepository
import com.genesyx.app.data.GenesyxProductRepository
import com.genesyx.app.data.MealLogRepository
import com.genesyx.app.data.PreferencesRepository
import com.genesyx.app.data.StreakRepository
import com.genesyx.app.data.SupplementReminderRepository
import com.genesyx.app.data.SupplementWriteResult
import com.genesyx.app.data.UserSupplementRepository
import com.genesyx.app.domain.content.PhaseFood
import com.genesyx.app.domain.content.nutritionPhaseDescription
import com.genesyx.app.domain.content.nutritionPhaseFoods
import com.genesyx.app.domain.content.phaseLabel
import com.genesyx.app.domain.cycle.CycleEngine
import com.genesyx.app.domain.hydration.HydrationCoach
import com.genesyx.app.domain.hydration.HydrationUnit
import com.genesyx.app.domain.model.GenesyxProduct
import com.genesyx.app.domain.model.MealEntry
import com.genesyx.app.domain.model.Phase
import com.genesyx.app.domain.model.UserSupplement
import com.genesyx.app.domain.streaks.StreakEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

data class NutritionUiState(
    val cycleSetUp: Boolean = false,
    val phase: Phase? = null,
    val phaseHeader: String = "TODAY · SET UP YOUR CYCLE",
    val headlineSub: String = "Set up your cycle to get personalised nutrition guidance.",
    val foods: List<PhaseFood> = emptyList(),
    val waterMl: Int = 0,
    /** Her goal, from preferences — [StreakEngine.DEFAULT_GOAL_ML] only until she sets her own. */
    val waterGoalMl: Int = StreakEngine.DEFAULT_GOAL_ML,
    /** Display unit for water amounts (ml/L or cups). Storage stays in ml. */
    val waterUnit: HydrationUnit = HydrationUnit.ML,
    /** Time-of-day pacing line for the hydration card — how today is going, right now. */
    val hydrationCoaching: String = "",
    val weeklyStreak: Int = 0,
    /** Days this week she actually hit [waterGoalMl], which is not the same as days she logged. */
    val daysOnGoal: Int = 0,
    /** Today's food-group tokens. The chips own this set. */
    val foodGroups: Set<String> = emptySet(),
)

@HiltViewModel
class NutritionViewModel @Inject constructor(
    private val cycleRepository: CycleRepository,
    private val dailyLogRepository: DailyLogRepository,
    private val preferencesRepository: PreferencesRepository,
    private val userSupplementRepository: UserSupplementRepository,
    private val supplementReminderRepository: SupplementReminderRepository,
    private val mealLogRepository: MealLogRepository,
    genesyxProductRepository: GenesyxProductRepository,
    streakRepository: StreakRepository,
) : ViewModel() {

    /** Today's logged meals — local-only, live from Room. */
    val todaysMeals: StateFlow<List<MealEntry>> =
        mealLogRepository.mealsForDate(LocalDate.now())
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Log a meal for today. Returns false if the description was blank. */
    fun logMeal(entry: MealEntry): Boolean = mealLogRepository.log(entry)

    fun deleteMeal(id: String) = mealLogRepository.delete(id)

    /** The user's own supplement entries — live from Room, synced in the background. */
    val userSupplements: StateFlow<List<UserSupplement>> = userSupplementRepository.supplements

    /** supplement id → daily reminder time (minutes-of-day); absent = no reminder set. */
    val supplementReminders: StateFlow<Map<String, Int>> = supplementReminderRepository.reminders

    init {
        // Re-arm the surviving reminders and drop any whose supplement was deleted, whenever the
        // list changes (and on first collection — the app-start re-arm).
        viewModelScope.launch {
            userSupplementRepository.supplements.collect { supplementReminderRepository.reconcile(it) }
        }
    }

    /** Set or clear a supplement's daily reminder. Minutes null = off. */
    fun setSupplementReminder(id: String, name: String, minutesOfDay: Int?) {
        if (minutesOfDay == null) supplementReminderRepository.clearReminder(id)
        else supplementReminderRepository.setReminder(id, name, minutesOfDay)
    }

    private val _catalogue = MutableStateFlow<List<GenesyxProduct>>(emptyList())

    /** The Genesyx range. Empty (→ "coming soon") for guests, offline, or while it has no SKUs. */
    val catalogue: StateFlow<List<GenesyxProduct>> = _catalogue.asStateFlow()

    init {
        viewModelScope.launch { _catalogue.value = genesyxProductRepository.fetchCatalogue() }
    }

    fun saveSupplement(entry: UserSupplement): SupplementWriteResult =
        if (entry.id in userSupplements.value.map { it.id }) {
            userSupplementRepository.update(entry)
        } else {
            userSupplementRepository.create(entry)
        }

    fun deleteSupplement(id: String) = userSupplementRepository.delete(id)

    fun addFromCatalogue(product: GenesyxProduct) {
        userSupplementRepository.create(
            UserSupplement(name = product.name, dose = product.dose, productId = product.id),
        )
    }

    val uiState: StateFlow<NutritionUiState> =
        combine(
            cycleRepository.settings,
            dailyLogRepository.logByDate,
            streakRepository.state,
            preferencesRepository.hydrationGoalMl,
            preferencesRepository.hydrationUnit,
        ) { settings, logs, streaks, goalMl, unit ->
            val today = LocalDate.now()
            // From the emitted map, not a `.value` side-read — the card must show the same total
            // every other collector of logByDate shows at the same instant.
            val waterMl = logs[today]?.waterMl ?: 0
            val foodGroups = logs[today]?.foodGroups.orEmpty()
            val coaching = HydrationCoach.coach(waterMl, goalMl, LocalTime.now(), unit).message
            if (settings == null) {
                NutritionUiState(
                    waterMl = waterMl,
                    waterGoalMl = goalMl,
                    waterUnit = unit,
                    hydrationCoaching = coaching,
                    weeklyStreak = streaks.weeklyStreak,
                    daysOnGoal = streaks.daysOnGoal,
                    foodGroups = foodGroups,
                )
            } else {
                val phase = CycleEngine.getCyclePhase(settings, today).phase
                NutritionUiState(
                    cycleSetUp = true,
                    phase = phase,
                    phaseHeader = "TODAY · ${phaseLabel.getValue(phase).uppercase()}",
                    headlineSub = nutritionPhaseDescription.getValue(phase),
                    foods = nutritionPhaseFoods.getValue(phase),
                    waterMl = waterMl,
                    waterGoalMl = goalMl,
                    waterUnit = unit,
                    hydrationCoaching = coaching,
                    weeklyStreak = streaks.weeklyStreak,
                    daysOnGoal = streaks.daysOnGoal,
                    foodGroups = foodGroups,
                )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = NutritionUiState(),
        )

    fun adjustWater(deltaMl: Int) = dailyLogRepository.adjustWater(deltaMl)

    fun toggleFoodGroup(id: String) = dailyLogRepository.toggleFoodGroup(id)

    fun logFoodGroups(ids: Set<String>) = dailyLogRepository.logFoodGroups(ids)

    fun setWaterGoal(goalMl: Int) = preferencesRepository.setHydrationGoalMl(goalMl)

    /** ml/cups display choice — shared app-wide via preferences. */
    fun setWaterUnit(unit: HydrationUnit) = preferencesRepository.setHydrationUnit(unit)

    /** Her glass — what one tap of the stepper pours. Shared app-wide via preferences. */
    val glassMl: StateFlow<Int> = preferencesRepository.hydrationGlassMl
    fun setGlassMl(ml: Int) = preferencesRepository.setHydrationGlassMl(ml)
}
