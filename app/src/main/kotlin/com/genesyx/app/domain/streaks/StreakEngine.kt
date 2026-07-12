package com.genesyx.app.domain.streaks

import com.genesyx.app.domain.model.DailyLog
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** A one-shot celebration. The ids are stable — they persist in preferences. */
enum class Milestone(val id: String) {
    DAY_7("milestone_7"),
    DAY_14("milestone_14"),
    WEEK_1("milestone_w1"),
    WEEK_4("milestone_w4"),
}

data class StreakState(
    val dailyHydration: Int = 0,
    val weeklyStreak: Int = 0,
    val daysLoggedThisWeek: Int = 0,
    /** Mon..Sun of the current week; true where anything at all was logged. */
    val weekActivity: List<Boolean> = List(7) { false },
    val bestDailyStreak: Int = 0,
    /** Milestones whose threshold is met right now. */
    val earned: Set<Milestone> = emptySet(),
    /** Earned but not yet celebrated — what a notification would fire on. */
    val newMilestones: Set<Milestone> = emptySet(),
)

/**
 * Pure streak computation — repositories feed it, nothing async happens inside, so it unit-tests
 * without coroutines (same shape as [com.genesyx.app.domain.cycle.CycleEngine]).
 *
 * Two streaks, deliberately different:
 * - **Daily hydration** — consecutive days back from today with any water. Unchanged semantics from
 *   `DailyLogRepository.streak`, which this replaces.
 * - **Weekly** — consecutive weeks in which she logged *anything* on at least [WEEK_COMPLETE_DAYS]
 *   of the seven days. Five-of-seven, not seven-of-seven: missing a day is meant to cost almost
 *   nothing. A week still in progress can't break the streak — an incomplete current week simply
 *   isn't counted yet.
 */
object StreakEngine {

    /** Days of logging that make a week count. */
    const val WEEK_COMPLETE_DAYS = 5

    fun compute(
        logsByDate: Map<LocalDate, DailyLog>,
        phByDate: Set<LocalDate>,
        today: LocalDate,
        celebrated: Set<String> = emptySet(),
        bestSoFar: Int = 0,
    ): StreakState {
        val activeDates = logsByDate.filterValues { it.hasActivity() }.keys + phByDate

        var dailyHydration = 0
        var day = today
        while ((logsByDate[day]?.waterMl ?: 0) > 0) {
            dailyHydration++
            day = day.minusDays(1)
        }

        val thisWeek = weekStart(today)
        val weekActivity = (0L until 7L).map { thisWeek.plusDays(it) in activeDates }

        val weeklyStreak = countCompleteWeeks(activeDates, thisWeek)

        val best = maxOf(bestSoFar, dailyHydration)
        val earned = buildSet {
            if (dailyHydration >= 7) add(Milestone.DAY_7)
            if (dailyHydration >= 14) add(Milestone.DAY_14)
            if (weeklyStreak >= 1) add(Milestone.WEEK_1)
            if (weeklyStreak >= 4) add(Milestone.WEEK_4)
        }

        return StreakState(
            dailyHydration = dailyHydration,
            weeklyStreak = weeklyStreak,
            daysLoggedThisWeek = weekActivity.count { it },
            weekActivity = weekActivity,
            bestDailyStreak = best,
            earned = earned,
            newMilestones = earned.filterNot { it.id in celebrated }.toSet(),
        )
    }

    /**
     * Walks back week by week from the current week, or from last week when the current one hasn't
     * reached the threshold yet (it may still get there). Bounded by the oldest logged day.
     */
    private fun countCompleteWeeks(activeDates: Set<LocalDate>, thisWeek: LocalDate): Int {
        val earliest = activeDates.minOrNull()?.let(::weekStart) ?: return 0
        var cursor = if (isCompleteWeek(activeDates, thisWeek)) thisWeek else thisWeek.minusWeeks(1)
        var weeks = 0
        while (!cursor.isBefore(earliest) && isCompleteWeek(activeDates, cursor)) {
            weeks++
            cursor = cursor.minusWeeks(1)
        }
        return weeks
    }

    private fun isCompleteWeek(activeDates: Set<LocalDate>, weekStart: LocalDate): Boolean =
        (0L until 7L).count { weekStart.plusDays(it) in activeDates } >= WEEK_COMPLETE_DAYS

    private fun weekStart(date: LocalDate): LocalDate =
        date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    /** Any tracked field counts — water, mood, energy, a symptom, sleep, supplements or a note. */
    private fun DailyLog.hasActivity(): Boolean =
        waterMl > 0 ||
            mood != null ||
            energy != null ||
            symptoms.isNotEmpty() ||
            sleepMinutes != null ||
            supplements.isNotEmpty() ||
            !notes.isNullOrBlank()
}
