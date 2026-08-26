package com.genesyx.app.ui.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.genesyx.app.data.CycleRepository
import com.genesyx.app.data.DailyLogRepository
import com.genesyx.app.data.PhRepository
import com.genesyx.app.data.PreferencesRepository
import com.genesyx.app.data.StreakRepository
import com.genesyx.app.data.UserSupplementRepository
import com.genesyx.app.domain.cycle.CycleEngine
import com.genesyx.app.domain.model.DayType
import com.genesyx.app.domain.model.Phase
import com.genesyx.app.domain.model.Supplement
import com.genesyx.app.domain.ph.PhStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

enum class Trend { UP, DOWN, FLAT }

data class WeeklySummaryInsights(
    val hasData: Boolean = false,
    /** Meaningful days this week (0..7) and the same figure for the week before. */
    val daysLogged: Int = 0,
    val prevDaysLogged: Int = 0,
    /** Each delta is null when either week lacks the data to compare — the card then omits it. */
    val hydrationDeltaMlPerDay: Int? = null,
    val sleepDeltaMinutes: Int? = null,
    val supplementDaysDelta: Int? = null,
    /** "Mostly good, energy often high" — empty when neither mood nor energy was logged. */
    val moodEnergyLine: String = "",
    val insight: String = "Log a day or two and your week in review will appear here.",
    /** Days this week she recorded at least one known food group. */
    val daysWithMeals: Int = 0,
)

data class PhInsights(
    val hasReadings: Boolean = false,
    val currentValue: Double? = null,
    val currentStatus: PhStatus? = null,
    val trend: Trend = Trend.FLAT,
    val avg7: Double? = null,
    val avg30: Double? = null,
    val readings30: Int = 0,
    val insight: String = "Log a few pH readings to see personalised insights here.",
    val recommendation: String = "",
)

data class ConsistencyInsights(
    val dailyStreak: Int = 0,
    val weeklyStreak: Int = 0,
    val daysLoggedThisWeek: Int = 0,
    val weekActivity: List<Boolean> = List(7) { false },
    val bestDailyStreak: Int = 0,
    val insight: String = "Log anything at all — water, a mood, a symptom — and this week starts counting.",
)

data class HydrationInsights(
    val hasData: Boolean = false,
    /** Mon..Sun of the current week, as a percentage of the daily goal. */
    val bars: List<Int> = List(7) { 0 },
    val avgMlPerDay: Int? = null,
    val deltaMlPerDay: Int? = null,
    /** Days this Mon–Sun week she hit her goal — StreakEngine's number, the same one Home shows. */
    val daysOnGoal: Int = 0,
    val insight: String = "Log your water for a few days and your hydration pattern will show up here.",
)

/** One chip on the consistency card's "today" row: a toggle-set entry and whether it is logged. */
data class SupplementTodayItem(val name: String, val logged: Boolean)

data class SupplementInsights(
    /** False only if her plan is empty — the card then invites her to set one rather than show 0/0. */
    val hasPlan: Boolean = true,
    val hasData: Boolean = false,
    /** Mon..Sun of the current week, as a percentage of her plan taken that day. */
    val bars: List<Int> = List(7) { 0 },
    val daysLogged: Int = 0,
    val suppTotal: Int = 0,
    /** The toggle set's size — the bundled plan plus her own entries; the Nutrition card's "M". */
    val planSize: Int = Supplement.defaultPlan.size,
    /** Today, from the same row the Nutrition card toggles — its "N". */
    val todayTaken: Int = 0,
    val todayItems: List<SupplementTodayItem> = emptyList(),
    val insight: String = "No supplements logged yet this week — even one, whenever you remember, is a gentle start.",
)

data class SleepInsights(
    val hasData: Boolean = false,
    /** Mon..Sun of the current week, as a percentage of a soft 10-hour ceiling. */
    val bars: List<Int> = List(7) { 0 },
    /** Averaged over the nights she logged, never over seven — see [SleepInsightLogic]. */
    val nightlyAverageMinutes: Int? = null,
    val nightsLogged: Int = 0,
    val insight: String = "Log a few nights' sleep and your week will show up here.",
)

data class CycleRegularityInsights(
    val hasData: Boolean = false,
    val cycleLength: Int? = null,
    val inTypicalRange: Boolean = false,
    val insight: String = "Set up your cycle and this card will show your place in it.",
    /** The engine's phases run-length-encoded over one cycle — two segments, not four, when a long
     *  period swallows the ovulation day. The card renders whatever it gets. */
    val segments: List<PhaseSegment> = emptyList(),
    val dayOfCycle: Int = 0,
    val currentPhase: Phase = Phase.FOLLICULAR,
    val nextPhase: Phase = Phase.PERIOD,
    val daysUntilNextPhase: Int = 0,
    /** 0..1 along the bar's widened visual weights — see [CycleRegularityLogic.markerFraction]. */
    val markerFraction: Float = 0f,
    val phaseInsightLine: String = "",
)

data class SymptomPatternInsights(
    /** 28 values, oldest first — the number of symptoms logged on each of the last 28 days. */
    val heatmapValues: List<Int> = List(SymptomPatternLogic.WINDOW_DAYS) { 0 },
    val daysWithSymptoms: Int = 0,
    val topSymptom: String? = null,
    val topCount: Int = 0,
    /** False below [SymptomPatternLogic.MIN_DAYS_FOR_PATTERN] days — the card then names no pattern. */
    val hasEnoughData: Boolean = false,
    val insight: String = "No symptoms logged yet.",
)

data class OvulationInsights(
    val hasData: Boolean = false,
    val cycleLength: Int = CycleEngine.DEFAULT_CYCLE_LENGTH,
    val currentDayOfCycle: Int = 0,
    val currentPhase: Phase = Phase.FOLLICULAR,
    val ovulationDay: Int = 0,
    /** All predictions, all arithmetic on her last period date — never a confirmed reading. */
    val ovulationDate: LocalDate? = null,
    val fertileWindowStart: LocalDate? = null,
    val fertileWindowEnd: LocalDate? = null,
    /** One entry per day of the cycle, classified by the same engine that paints the Track calendar. */
    val dayTypes: List<DayType> = emptyList(),
    val insight: String = "Set up your cycle and Genesyx will estimate your fertile window.",
)

@HiltViewModel
class InsightsViewModel @Inject constructor(
    phRepository: PhRepository,
    dailyLogRepository: DailyLogRepository,
    cycleRepository: CycleRepository,
    streakRepository: StreakRepository,
    preferencesRepository: PreferencesRepository,
    userSupplementRepository: UserSupplementRepository,
) : ViewModel() {

    val weeklySummary: StateFlow<WeeklySummaryInsights> =
        dailyLogRepository.logByDate
            .map { logs -> WeeklySummaryLogic.compute(logs) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = WeeklySummaryInsights(),
            )

    val phInsights: StateFlow<PhInsights> =
        phRepository.readings
            .map { readings -> PhInsightLogic.compute(readings) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = PhInsights(),
            )

    val consistencyInsights: StateFlow<ConsistencyInsights> =
        streakRepository.state
            .map { streaks -> ConsistencyInsightLogic.compute(streaks) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = ConsistencyInsights(),
            )

    // Her goal, not a constant. The bars are a percentage of what she asked for.
    val hydrationInsights: StateFlow<HydrationInsights> =
        combine(
            dailyLogRepository.logByDate,
            preferencesRepository.hydrationGoalMl,
            preferencesRepository.hydrationUnit,
        ) { logs, goalMl, unit ->
            HydrationInsightLogic.compute(logs, goalMl = goalMl, unit = unit)
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = HydrationInsights(),
            )

    /**
     * Reads the one repository the Nutrition tab's chips write ([DailyLogRepository.logByDate])
     * and the same custom entries that join its toggle set — there is no second query path that
     * could disagree with the "N of M" she just saw there.
     */
    val supplementInsights: StateFlow<SupplementInsights> =
        combine(
            dailyLogRepository.logByDate,
            userSupplementRepository.supplements,
        ) { logs, custom -> SupplementInsightLogic.compute(logs, custom = custom) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = SupplementInsights(),
            )

    /** Her own "Your supplements" (from Nutrition), with each one's this-week log count. */
    val userSupplementInsights: StateFlow<UserSupplementInsights> =
        combine(
            userSupplementRepository.supplements,
            dailyLogRepository.logByDate,
        ) { supplements, logs ->
            UserSupplementInsightLogic.compute(supplements.map { it.name }, logs)
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = UserSupplementInsights(),
            )

    val sleepInsights: StateFlow<SleepInsights> =
        dailyLogRepository.logByDate
            .map { logs -> SleepInsightLogic.compute(logs) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = SleepInsights(),
            )

    /** Private intimacy, this week — hidden until she has recorded it at least once. */
    val intimacyInsights: StateFlow<IntimacyInsights> =
        dailyLogRepository.logByDate
            .map { logs -> IntimacyInsightLogic.compute(logs) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = IntimacyInsights(),
            )

    val symptomInsights: StateFlow<SymptomPatternInsights> =
        dailyLogRepository.logByDate
            .map { logs -> SymptomPatternLogic.compute(logs) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = SymptomPatternInsights(),
            )

    // Both cycle cards read the one saved setup. Neither claims cycle-to-cycle history, because the
    // app stores none — see CycleRegularityLogic.
    val cycleRegularityInsights: StateFlow<CycleRegularityInsights> =
        cycleRepository.settings
            .map { settings -> CycleRegularityLogic.compute(settings) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = CycleRegularityInsights(),
            )

    val ovulationInsights: StateFlow<OvulationInsights> =
        cycleRepository.settings
            .map { settings -> OvulationLogic.compute(settings) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = OvulationInsights(),
            )
}
