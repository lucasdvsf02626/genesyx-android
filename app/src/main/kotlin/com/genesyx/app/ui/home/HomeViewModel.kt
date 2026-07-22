package com.genesyx.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.genesyx.app.data.CycleRepository
import com.genesyx.app.data.DailyLogRepository
import com.genesyx.app.data.PhRepository
import com.genesyx.app.data.PreferencesRepository
import com.genesyx.app.data.SessionRepository
import com.genesyx.app.data.StreakRepository
import com.genesyx.app.domain.content.phaseHeroCopy
import com.genesyx.app.domain.content.phaseHeroSubtext
import com.genesyx.app.domain.content.phaseHeroText
import com.genesyx.app.domain.content.phaseSubLabel
import com.genesyx.app.domain.content.phaseTags
import com.genesyx.app.domain.cycle.CycleEngine
import com.genesyx.app.domain.hydration.HydrationCoach
import com.genesyx.app.domain.hydration.HydrationPace
import com.genesyx.app.domain.model.CycleSettings
import com.genesyx.app.domain.model.PhMeasurement
import com.genesyx.app.domain.model.PhReading
import com.genesyx.app.domain.streaks.StreakEngine
import com.genesyx.app.domain.streaks.StreakState
import com.genesyx.app.domain.time.WeekBuckets
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

/** Home screen UI state. Mirrors the web Home (greeting, cycle hero, hydration, streak). */
data class HomeUiState(
    val userName: String = "Guest",
    val signedIn: Boolean = false,
    val greeting: String = "Good morning",
    val settings: CycleSettings? = null,
    val cycleSetUp: Boolean = false,
    val cycleEyebrow: String = "TODAY",
    val cycleHeadline: String = "Set up your cycle",
    val cycleSub: String = "Add your last period date to get personalised insights.",
    val cycleTags: List<String> = emptyList(),
    // Cycle-hero metric row (only meaningful once cycle is set up).
    val cycleDay: Int? = null,
    val daysToNextLabel: String? = null,
    val ovulationDayLabel: String? = null,
    val todayFocusTitle: String? = null,
    val todayFocusBody: String? = null,
    // Hydration summary card.
    val hydrationLitres: Float? = null,
    /** Her goal, from preferences — the default only until she sets her own. */
    val hydrationGoalLitres: Float = StreakEngine.DEFAULT_GOAL_ML / 1000f,
    val hydrationPercent: Int = 0,
    val hydrationPace: HydrationPace = HydrationPace.NOT_STARTED,
    val hydrationStreak: Int = 0,
    /** Mon..Sun of the current week: true where that day hit the goal. */
    val weekOnGoal: List<Boolean> = List(7) { false },
    val daysOnGoal: Int = 0,
    val hydrationCoaching: String? = null,
    // pH nudge card — the latest reading value, or null when none exists.
    val phLatest: Double? = null,
    /** True when that latest reading is a pre-migration urine reading, so the card marks it legacy. */
    val phLatestIsLegacy: Boolean = false,
    val streakDays: Int? = null,
    val isLoading: Boolean = false,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val cycleRepository: CycleRepository,
    private val dailyLogRepository: DailyLogRepository,
    private val sessionRepository: SessionRepository,
    private val preferencesRepository: PreferencesRepository,
    private val phRepository: PhRepository,
    streakRepository: StreakRepository,
) : ViewModel() {

    // Paired rather than passed as separate flows: combine is only typed up to five.
    private val streaksWithGoal =
        combine(streakRepository.state, preferencesRepository.hydrationGoalMl) { streaks, goalMl -> streaks to goalMl }
    private val sessionInfo =
        combine(sessionRepository.displayName, sessionRepository.isSignedIn) { name, signed -> name to signed }

    val uiState: StateFlow<HomeUiState> =
        combine(
            cycleRepository.settings,
            dailyLogRepository.logByDate,
            sessionInfo,
            streaksWithGoal,
            phRepository.readings,
        ) { settings, logs, (displayName, signedIn), (streaks, goalMl), readings ->
            buildState(settings, logs, displayName, signedIn, streaks, goalMl, readings)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = buildState(
                cycleRepository.settings.value,
                dailyLogRepository.logByDate.value,
                sessionRepository.displayName.value,
                sessionRepository.isSignedIn.value,
                streakRepository.state.value,
                preferencesRepository.hydrationGoalMl.value,
                phRepository.readings.value,
            ),
        )

    fun saveCycleSettings(settings: CycleSettings) = cycleRepository.upsert(settings)

    private fun buildState(
        settings: CycleSettings?,
        logs: Map<LocalDate, com.genesyx.app.domain.model.DailyLog>,
        displayName: String?,
        signedIn: Boolean,
        streaks: StreakState,
        goalMl: Int,
        readings: List<PhReading>,
    ): HomeUiState {
        val today = LocalDate.now()
        val waterMl = logs[today]?.waterMl ?: 0
        val coaching = HydrationCoach.coach(waterMl, goalMl, LocalTime.now())
        val weekOnGoal = WeekBuckets.weekDays(today).map { (logs[it]?.waterMl ?: 0) >= goalMl }

        val base = HomeUiState(
            userName = displayName ?: "Guest",
            signedIn = signedIn,
            greeting = greetingFor(LocalTime.now()),
            settings = settings,
            hydrationLitres = if (waterMl > 0) waterMl / 1000f else null,
            hydrationGoalLitres = goalMl / 1000f,
            hydrationPercent = (waterMl * 100 / goalMl).coerceIn(0, 100),
            hydrationPace = coaching.pace,
            hydrationStreak = streaks.dailyHydration,
            weekOnGoal = weekOnGoal,
            daysOnGoal = streaks.daysOnGoal,
            hydrationCoaching = coaching.message,
            phLatest = readings.maxByOrNull { it.recordedAt }?.phValue,
            phLatestIsLegacy = readings.maxByOrNull { it.recordedAt }?.measurementType == PhMeasurement.URINE,
            // Any logged activity, not water alone — the card is labelled "Streak", so it has to
            // count everything she tracks, and it must not reset at midnight.
            streakDays = streaks.dailyActivity,
        )
        if (settings == null) return base

        val info = CycleEngine.getCyclePhase(settings, today)
        val inFertile = info.dayOfCycle in info.fertileWindow
        val focus = phaseHeroCopy.getValue(info.phase).focus
        return base.copy(
            cycleSetUp = true,
            cycleEyebrow = "DAY ${info.dayOfCycle} · ${phaseSubLabel(info.phase, inFertile).uppercase()}",
            cycleHeadline = phaseHeroText(info.phase, inFertile),
            cycleSub = phaseHeroSubtext(info.phase, inFertile),
            cycleTags = phaseTags(info.phase, inFertile),
            cycleDay = info.dayOfCycle,
            daysToNextLabel = if (info.daysUntilNextPeriod == 0) "Today" else "${info.daysUntilNextPeriod} days",
            ovulationDayLabel = "Day ${info.ovulationDay}",
            todayFocusTitle = focus.title,
            todayFocusBody = focus.body,
        )
    }

    private fun greetingFor(time: LocalTime): String = when (time.hour) {
        in 0..11 -> "Good morning"
        in 12..17 -> "Good afternoon"
        else -> "Good evening"
    }
}
