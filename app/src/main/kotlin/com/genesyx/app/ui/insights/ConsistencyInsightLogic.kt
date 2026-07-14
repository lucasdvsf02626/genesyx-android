package com.genesyx.app.ui.insights

import com.genesyx.app.domain.streaks.StreakState

/**
 * Turns a [StreakState] into the copy for the Consistency card. Split from the card so the wording
 * — the part that has to stay encouraging whatever the numbers do — is unit-tested.
 */
object ConsistencyInsightLogic {

    fun compute(streaks: StreakState): ConsistencyInsights = ConsistencyInsights(
        dailyStreak = streaks.dailyHydration,
        weeklyStreak = streaks.weeklyStreak,
        daysLoggedThisWeek = streaks.daysLoggedThisWeek,
        weekActivity = streaks.weekActivity,
        bestDailyStreak = streaks.bestDailyStreak,
        insight = insightFor(streaks),
    )

    /** A missed day is never framed as a failure — the copy only ever counts what she has done. */
    private fun insightFor(streaks: StreakState): String {
        if (streaks.daysLoggedThisWeek == 0 && streaks.weeklyStreak == 0) {
            return "Log anything at all — water, a mood, a symptom — and this week starts counting."
        }
        val days = "You've logged ${streaks.daysLoggedThisWeek} of 7 days this week"
        return when (streaks.weeklyStreak) {
            0 -> "$days. Five days makes the week count."
            1 -> "$days — one steady week behind you."
            else -> "$days — ${streaks.weeklyStreak} steady weeks behind you."
        }
    }
}
