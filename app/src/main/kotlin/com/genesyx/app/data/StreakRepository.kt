package com.genesyx.app.data

import com.genesyx.app.core.di.ApplicationScope
import com.genesyx.app.domain.streaks.Milestone
import com.genesyx.app.domain.streaks.StreakEngine
import com.genesyx.app.domain.streaks.StreakState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Feeds [StreakEngine] from the daily logs, the pH readings and the persisted counters, and pushes a
 * new all-time best back to preferences as it is set. `bestDailyStreak` is an input as well as an
 * output so a value arriving late from DataStore recomputes the state; the write-back converges
 * because it only fires while the current streak is longer than the stored best.
 */
@Singleton
class StreakRepository @Inject constructor(
    dailyLogRepository: DailyLogRepository,
    phRepository: PhRepository,
    private val preferences: PreferencesRepository,
    @ApplicationScope private val scope: CoroutineScope,
) {
    // Two nested combines because the typed `combine` maxes out at 5 flows and we now feed 6
    // (article-read dates joined the streak inputs — a read is a meaningful action).
    private val logSources = combine(
        dailyLogRepository.logByDate,
        phRepository.readings,
        preferences.articleReadDates,
    ) { logs, readings, articleDates -> Triple(logs, readings, articleDates) }

    private val prefSources = combine(
        preferences.bestDailyStreak,
        preferences.celebratedMilestones,
        preferences.hydrationGoalMl,
    ) { best, celebrated, goalMl -> Triple(best, celebrated, goalMl) }

    val state: StateFlow<StreakState> =
        combine(logSources, prefSources) { (logs, readings, articleDates), (best, celebrated, goalMl) ->
            StreakEngine.compute(
                logsByDate = logs,
                phByDate = readings.map { it.recordedAt.toLocalDate() }.toSet(),
                today = LocalDate.now(),
                celebrated = celebrated,
                bestSoFar = best,
                goalMl = goalMl,
                articleReadDates = articleDates,
            )
        }
            .onEach { streaks ->
                if (streaks.dailyHydration > preferences.bestDailyStreak.value) {
                    preferences.setBestDailyStreak(streaks.dailyHydration)
                }
            }
            .stateIn(scope, SharingStarted.Eagerly, StreakState())

    /**
     * Records which milestones have been celebrated. Pass the currently-earned set: milestones that
     * are no longer earned drop out, so falling below a threshold and climbing back re-fires it.
     */
    fun markCelebrated(milestones: Set<Milestone>) {
        preferences.setCelebratedMilestones(milestones.map { it.id }.toSet())
    }
}
