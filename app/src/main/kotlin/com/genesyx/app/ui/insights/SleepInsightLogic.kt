package com.genesyx.app.ui.insights

import com.genesyx.app.domain.model.DailyLog
import com.genesyx.app.domain.time.WeekBuckets
import java.time.LocalDate

/**
 * Pure sleep computation for the current Mon–Sun week — the same week [WeekBuckets] gives Hydration
 * and Supplements, so the three cards can never disagree about which day a week begins on.
 *
 * The average divides by nights she *logged*, not by seven. Hydration does the opposite (an unlogged
 * day really is zero millilitres drunk), but an unlogged night is not a night of no sleep — it is a
 * night she didn't reach for her phone. Dividing by seven would turn four good nights into a report
 * of four hours a night, which is both wrong and unkind.
 */
object SleepInsightLogic {

    /** The bar ceiling: ten hours. A soft scale, not a goal — the card draws no goal line. */
    const val CEILING_MINUTES = 600

    fun compute(
        logsByDate: Map<LocalDate, DailyLog>,
        today: LocalDate = LocalDate.now(),
    ): SleepInsights {
        // A night counts only if it holds real sleep. `sleepMinutes` is nullable, and a stored zero
        // says the same thing a null does — nothing was recorded — so neither enters the average.
        val minutesPerNight = WeekBuckets.weekDays(today).map { date ->
            logsByDate[date]?.sleepMinutes?.takeIf { it > 0 }
        }

        val logged = minutesPerNight.filterNotNull()
        if (logged.isEmpty()) return SleepInsights()

        val average = logged.sum() / logged.size
        return SleepInsights(
            hasData = true,
            bars = minutesPerNight.map { ((it ?: 0) * 100 / CEILING_MINUTES).coerceIn(0, 100) },
            nightlyAverageMinutes = average,
            nightsLogged = logged.size,
            insight = insightFor(average, logged.size),
        )
    }

    /** Reports the number and stops. It is not the app's place to tell her she slept badly. */
    private fun insightFor(averageMinutes: Int, nightsLogged: Int): String {
        val nights = if (nightsLogged == 1) "the one night" else "the $nightsLogged nights"
        return "You're averaging ${formatDuration(averageMinutes)} a night across $nights you've " +
            "logged this week."
    }

    /** "7h 20m", "8h" — never "7.33 hours". */
    fun formatDuration(minutes: Int): String {
        val hours = minutes / 60
        val remainder = minutes % 60
        return when {
            hours == 0 -> "${remainder}m"
            remainder == 0 -> "${hours}h"
            else -> "${hours}h ${remainder}m"
        }
    }
}
