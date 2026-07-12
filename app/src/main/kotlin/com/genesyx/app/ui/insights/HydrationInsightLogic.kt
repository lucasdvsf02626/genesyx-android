package com.genesyx.app.ui.insights

import com.genesyx.app.domain.model.DailyLog
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import kotlin.math.abs

/**
 * Pure hydration insight computation, replacing the mocked "Nutrition consistency" card.
 *
 * The bars show the current Mon–Sun week, but the week-over-week delta compares two rolling 7-day
 * windows (the last seven days against the seven before them). Comparing calendar weeks would put a
 * two-day-old week next to a finished one and report a fake collapse; seven-against-seven is always
 * a like-for-like comparison. Unlogged days count as zero, so the average is millilitres per day
 * rather than per day-she-remembered.
 */
object HydrationInsightLogic {

    const val GOAL_ML = 2400

    fun compute(logsByDate: Map<LocalDate, DailyLog>, today: LocalDate = LocalDate.now()): HydrationInsights {
        fun waterOn(date: LocalDate) = logsByDate[date]?.waterMl ?: 0
        fun windowTotal(endingDaysAgo: Long) =
            (0L until 7L).sumOf { waterOn(today.minusDays(endingDaysAgo + it)) }

        val thisWindow = windowTotal(0)
        if (thisWindow == 0) return HydrationInsights()

        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val bars = (0L until 7L).map { day ->
            (waterOn(weekStart.plusDays(day)) * 100 / GOAL_ML).coerceIn(0, 100)
        }

        val avgMlPerDay = thisWindow / 7
        val previousWindow = windowTotal(7)
        val deltaMlPerDay = if (previousWindow == 0) null else avgMlPerDay - previousWindow / 7

        return HydrationInsights(
            hasData = true,
            bars = bars,
            avgMlPerDay = avgMlPerDay,
            deltaMlPerDay = deltaMlPerDay,
            insight = insightFor(avgMlPerDay, deltaMlPerDay),
        )
    }

    /** Never scolds a dip — it reports the number and leaves it there. */
    private fun insightFor(avgMlPerDay: Int, deltaMlPerDay: Int?): String {
        val average = "You're averaging ${avgMlPerDay}ml a day"
        val change = when {
            deltaMlPerDay == null -> " — another week of logging and you'll see the change week to week."
            deltaMlPerDay >= 50 -> ", ${deltaMlPerDay}ml more than the previous seven days."
            deltaMlPerDay <= -50 -> ", ${abs(deltaMlPerDay)}ml less than the previous seven days."
            else -> ", holding steady with the previous seven days."
        }
        return average + change
    }
}
