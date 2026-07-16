package com.genesyx.app.domain.hydration

import java.time.LocalTime

/** How today's water is going relative to where the day is. Never a grade — a position. */
enum class HydrationPace { NOT_STARTED, BEHIND, ON_TRACK, AHEAD, REACHED }

data class HydrationCoaching(
    val pace: HydrationPace,
    /** Time-of-day-aware, non-shaming copy for the hydration card. */
    val message: String,
)

/**
 * Intraday hydration coaching: it compares how much she's drunk with how much of the day has passed,
 * and frames the result by part of day. This is the piece the weekly Hydration insight can't be —
 * that one looks back over seven days; this one is about *right now*, at 3pm, half a litre in.
 *
 * The pacing curve is linear across a waking window ([START_HOUR]..[END_HOUR]). Before the window
 * there is no expectation at all — a fresh morning is not "behind" — and after it the full goal is
 * expected. The voice never scolds a shortfall; being behind reads as an easy invitation, not a miss.
 */
object HydrationCoach {

    /** The waking window the day's pace is measured across. Outside it, expectation is 0 or 1. */
    const val START_HOUR = 8
    const val END_HOUR = 22

    /** How far below the day's expected pace still counts as "on track" — a whole day is noisy. */
    private const val TOLERANCE = 0.12

    fun coach(currentMl: Int, goalMl: Int, now: LocalTime = LocalTime.now()): HydrationCoaching {
        val goal = goalMl.coerceAtLeast(1)
        val remaining = (goal - currentMl).coerceAtLeast(0)

        if (currentMl >= goal) {
            return HydrationCoaching(HydrationPace.REACHED, "You've reached your water goal today — lovely.")
        }

        val expected = expectedFraction(now)
        if (expected <= 0.0) {
            return HydrationCoaching(
                HydrationPace.NOT_STARTED,
                "${partOfDay(now)} is a fresh start — a glass now sets an easy pace.",
            )
        }

        val actual = currentMl.toDouble() / goal
        val pace = when {
            actual > expected + TOLERANCE -> HydrationPace.AHEAD
            actual >= expected - TOLERANCE -> HydrationPace.ON_TRACK
            else -> HydrationPace.BEHIND
        }
        return HydrationCoaching(pace, message(pace, now, remaining))
    }

    /** The fraction of the goal the day "expects" by [now] — 0 before the window, 1 after it. */
    fun expectedFraction(now: LocalTime): Double {
        val hour = now.toSecondOfDay() / 3600.0
        return ((hour - START_HOUR) / (END_HOUR - START_HOUR)).coerceIn(0.0, 1.0)
    }

    private fun message(pace: HydrationPace, now: LocalTime, remainingMl: Int): String {
        val part = partOfDay(now)
        return when (pace) {
            HydrationPace.AHEAD -> "$part you're ahead of the day's pace — nicely done."
            HydrationPace.ON_TRACK -> "$part you're right on pace, about ${remainingMl}ml to go."
            HydrationPace.BEHIND -> "$part you're a little behind the day's pace — a glass whenever suits, no rush."
            // The two edge paces don't route here, but keep the when exhaustive and honest.
            HydrationPace.REACHED -> "You've reached your water goal today — lovely."
            HydrationPace.NOT_STARTED -> "$part is a fresh start — a glass now sets an easy pace."
        }
    }

    private fun partOfDay(now: LocalTime): String = when {
        now.hour < 12 -> "This morning"
        now.hour < 17 -> "This afternoon"
        else -> "This evening"
    }
}
