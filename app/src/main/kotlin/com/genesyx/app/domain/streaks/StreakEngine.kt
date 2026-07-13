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
    /** Consecutive days ending today on which *anything* was logged. What Home calls the streak. */
    val dailyActivity: Int = 0,
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
 * Three streaks, deliberately different:
 * - **Daily activity** — consecutive days ending today on which anything at all was logged. This is
 *   the one Home shows: a user who logs her mood and sleep every day is on a streak, and it would
 *   be a lie to tell her otherwise just because she didn't record water.
 * - **Daily hydration** — the same run, but water only. Surfaced on the Insights consistency card.
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

        val dailyActivity = runEndingToday(today) { it in activeDates }
        val dailyHydration = runEndingToday(today) { (logsByDate[it]?.waterMl ?: 0) > 0 }

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
            dailyActivity = dailyActivity,
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
     * Consecutive days ending today on which [logged] holds.
     *
     * A day that hasn't been logged *yet* must not read as a broken streak. Today is undecided
     * until it is over, so the run is measured through yesterday and today's log extends it.
     * Counting from today instead would zero every streak at midnight and restore it only once she
     * logged again — so a user with a 30-day run saw "0 days" every single morning.
     */
    private inline fun runEndingToday(today: LocalDate, logged: (LocalDate) -> Boolean): Int {
        var day = if (logged(today)) today else today.minusDays(1)
        var count = 0
        while (logged(day)) {
            count++
            day = day.minusDays(1)
        }
        return count
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
