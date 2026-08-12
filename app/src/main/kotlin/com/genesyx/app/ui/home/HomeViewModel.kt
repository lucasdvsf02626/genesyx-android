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
import com.genesyx.app.domain.hydration.HydrationUnit
import com.genesyx.app.domain.model.CycleSettings
import com.genesyx.app.domain.model.PhMeasurement
import com.genesyx.app.domain.model.PhReading
import com.genesyx.app.domain.content.Article
import com.genesyx.app.domain.content.ArticleCategory
import com.genesyx.app.domain.content.LearnDrip
import com.genesyx.app.domain.model.isMeaningful
import com.genesyx.app.domain.streaks.Milestone
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

/** Days of the "stay hydrated" challenge — log water 7 days running. */
const val HYDRATION_CHALLENGE_TARGET = 7

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
    val hydrationMl: Int? = null,
    /** Her goal, from preferences — the default only until she sets her own. */
    val hydrationGoalMl: Int = StreakEngine.DEFAULT_GOAL_ML,
    /** Display unit for water amounts. Storage stays in ml. */
    val hydrationUnit: HydrationUnit = HydrationUnit.ML,
    val hydrationPercent: Int = 0,
    val hydrationPace: HydrationPace = HydrationPace.NOT_STARTED,
    val hydrationStreak: Int = 0,
    /** Consecutive weeks that counted (4+ logged days) — the weekly streak, shown beside the daily one. */
    val weeklyStreak: Int = 0,
    /** Progress toward the 7-day hydration challenge: consecutive days with water, capped at 7. */
    val hydrationChallengeDays: Int = 0,
    /** Mon..Sun of the current week: true where that day hit the goal. */
    val weekOnGoal: List<Boolean> = List(7) { false },
    val daysOnGoal: Int = 0,
    val hydrationCoaching: String? = null,
    // pH nudge card — the latest reading value, or null when none exists.
    val phLatest: Double? = null,
    /** True when that latest reading is a pre-migration urine reading, so the card marks it legacy. */
    val phLatestIsLegacy: Boolean = false,
    val streakDays: Int? = null,
    /** Earned-but-uncelebrated milestones — non-empty pops the one-shot celebration dialog. */
    val newMilestones: Set<Milestone> = emptySet(),
    /** Yesterday, when logging it would reconnect a just-broken streak; null otherwise. */
    val restoreDate: LocalDate? = null,
    // "New article this week" card — null until a drip article she hasn't seen the card for exists.
    val newArticleSlug: String? = null,
    val newArticleTitle: String? = null,
    val isLoading: Boolean = false,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val cycleRepository: CycleRepository,
    private val dailyLogRepository: DailyLogRepository,
    private val sessionRepository: SessionRepository,
    private val preferencesRepository: PreferencesRepository,
    private val phRepository: PhRepository,
    private val streakRepository: StreakRepository,
) : ViewModel() {

    /** The non-streak, non-log inputs, paired up because combine is only typed to five flows. */
    private data class SessionAndLearn(
        val displayName: String?,
        val signedIn: Boolean,
        val firstOpenEpochDay: Long?,
        val lastSeenArticleSlug: String?,
    )

    // Paired rather than passed as separate flows: combine is only typed up to five.
    private val streaksWithGoal =
        combine(
            streakRepository.state,
            preferencesRepository.hydrationGoalMl,
            preferencesRepository.hydrationUnit,
        ) { streaks, goalMl, unit -> Triple(streaks, goalMl, unit) }
    private val sessionInfo =
        combine(
            sessionRepository.displayName,
            sessionRepository.isSignedIn,
            preferencesRepository.firstOpenEpochDay,
            preferencesRepository.lastSeenArticleSlug,
        ) { name, signed, firstOpen, lastSeen -> SessionAndLearn(name, signed, firstOpen, lastSeen) }

    val uiState: StateFlow<HomeUiState> =
        combine(
            cycleRepository.settings,
            dailyLogRepository.logByDate,
            sessionInfo,
            streaksWithGoal,
            phRepository.readings,
        ) { settings, logs, session, (streaks, goalMl, unit), readings ->
            buildState(settings, logs, session, streaks, goalMl, unit, readings)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = buildState(
                cycleRepository.settings.value,
                dailyLogRepository.logByDate.value,
                SessionAndLearn(
                    sessionRepository.displayName.value,
                    sessionRepository.isSignedIn.value,
                    preferencesRepository.firstOpenEpochDay.value,
                    preferencesRepository.lastSeenArticleSlug.value,
                ),
                streakRepository.state.value,
                preferencesRepository.hydrationGoalMl.value,
                preferencesRepository.hydrationUnit.value,
                phRepository.readings.value,
            ),
        )

    fun saveCycleSettings(settings: CycleSettings) = cycleRepository.upsert(settings)

    /**
     * Marks every currently-earned milestone celebrated (per [StreakRepository.markCelebrated]'s
     * contract: dropping below a threshold and climbing back re-fires it).
     */
    fun celebrateMilestones() = streakRepository.markCelebrated(streakRepository.state.value.earned)

    /** The "new article" card was acted on — don't show it again for this slug. */
    fun markArticleSeen(slug: String) = preferencesRepository.setLastSeenArticleSlug(slug)

    private fun buildState(
        settings: CycleSettings?,
        logs: Map<LocalDate, com.genesyx.app.domain.model.DailyLog>,
        session: SessionAndLearn,
        streaks: StreakState,
        goalMl: Int,
        unit: HydrationUnit,
        readings: List<PhReading>,
    ): HomeUiState {
        val today = LocalDate.now()
        val waterMl = logs[today]?.waterMl ?: 0
        val coaching = HydrationCoach.coach(waterMl, goalMl, LocalTime.now(), unit)
        val weekOnGoal = WeekBuckets.weekDays(today).map { (logs[it]?.waterMl ?: 0) >= goalMl }

        // A streak broken by exactly yesterday is restorable: filling yesterday reconnects the run
        // (the engine just recomputes). Any older gap is history, not a prompt.
        fun active(d: LocalDate) =
            logs[d]?.isMeaningful() == true || readings.any { it.recordedAt.toLocalDate() == d }
        val restoreDate = today.minusDays(1)
            .takeIf { yesterday -> !active(yesterday) && active(today.minusDays(2)) }

        // "A read for your week": the freshly released weekly-series article when one landed in the
        // last seven days, otherwise a deterministic weekly rotation so the block is always present
        // and always points at real, published content.
        val weeklyRead = LearnDrip.newestReleased(today) ?: weeklyRotation(today)

        val base = HomeUiState(
            userName = session.displayName ?: "Guest",
            signedIn = session.signedIn,
            greeting = greetingFor(LocalTime.now()),
            settings = settings,
            hydrationMl = if (waterMl > 0) waterMl else null,
            hydrationGoalMl = goalMl,
            hydrationUnit = unit,
            hydrationPercent = (waterMl * 100 / goalMl).coerceIn(0, 100),
            hydrationPace = coaching.pace,
            hydrationStreak = streaks.dailyHydration,
            weeklyStreak = streaks.weeklyStreak,
            // A rolling 7-day challenge: keep water logged 7 days running. Caps at 7 (a completed
            // challenge shows full), and simply rolls forward as the hydration streak grows.
            hydrationChallengeDays = streaks.dailyHydration.coerceAtMost(HYDRATION_CHALLENGE_TARGET),
            weekOnGoal = weekOnGoal,
            daysOnGoal = streaks.daysOnGoal,
            hydrationCoaching = coaching.message,
            phLatest = readings.maxByOrNull { it.recordedAt }?.phValue,
            phLatestIsLegacy = readings.maxByOrNull { it.recordedAt }?.measurementType == PhMeasurement.URINE,
            // Any logged activity, not water alone — the card is labelled "Streak", so it has to
            // count everything she tracks, and it must not reset at midnight.
            streakDays = streaks.dailyActivity,
            newMilestones = streaks.newMilestones,
            restoreDate = restoreDate,
            newArticleSlug = weeklyRead?.slug,
            newArticleTitle = weeklyRead?.title,
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

    /**
     * A deterministic "read for your week" for weeks with no fresh weekly-series drop: it advances
     * one article per ISO-ish week (epoch-day / 7) through the published editorial articles, skipping
     * the how-to guides so the Home block surfaces a genuine read rather than a manual. Deterministic,
     * so it's stable within a week and identical across a process restart — no randomness.
     */
    private fun weeklyRotation(today: LocalDate): Article? {
        val pool = LearnDrip.published(today).filter { it.category != ArticleCategory.GUIDES }
        if (pool.isEmpty()) return null
        return pool[((today.toEpochDay() / 7) % pool.size).toInt()]
    }

    private fun greetingFor(time: LocalTime): String = when (time.hour) {
        in 0..11 -> "Good morning"
        in 12..17 -> "Good afternoon"
        else -> "Good evening"
    }
}
