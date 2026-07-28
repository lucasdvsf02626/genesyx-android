package com.genesyx.app.ui.insights

import com.genesyx.app.domain.hydration.HydrationFormat
import com.genesyx.app.domain.hydration.HydrationUnit
import com.genesyx.app.domain.model.DailyLog
import com.genesyx.app.domain.streaks.StreakEngine
import com.genesyx.app.domain.time.WeekBuckets
import java.time.LocalDate
import kotlin.math.abs

/**
 * Pure hydration insight computation, replacing the mocked "Nutrition consistency" card.
 *
 * The bars show the current Mon–Sun week, but the week-over-week delta compares two rolling 7-day
 * windows (the last seven days against the seven before them). Comparing calendar weeks would put a
 * two-day-old week next to a finished one and report a fake collapse; seven-against-seven is always
 * a like-for-like comparison.
 *
 * The average divides by days she actually logged, not by seven: dividing a two-day 1000ml by the
 * calendar produced "142ml a day" — a meaninglessly tiny number that read as failure while the data
 * was merely sparse. The copy names the denominator ("on the days you log") so the number stays
 * honest. Mirrored in the iOS repo via `hydrationAverageCases` in tracking_test_vectors.json.
 */
object HydrationInsightLogic {

    /**
     * [goalMl] is her goal, not a constant. This used to hold a private `GOAL_ML = 2400` that
     * shadowed the goal she had actually set, so a woman on a 3000ml goal still had her bars scored
     * against 2400 — the two numbers happened to be equal by default, which is why it went unseen.
     * [PreferencesRepository][com.genesyx.app.data.PreferencesRepository] clamps to
     * [StreakEngine.GOAL_RANGE_ML], so this can never be zero and the division below is safe.
     */
    fun compute(
        logsByDate: Map<LocalDate, DailyLog>,
        today: LocalDate = LocalDate.now(),
        goalMl: Int = StreakEngine.DEFAULT_GOAL_ML,
        unit: HydrationUnit = HydrationUnit.ML,
    ): HydrationInsights {
        fun waterOn(date: LocalDate) = logsByDate[date]?.waterMl ?: 0
        fun windowDates(endingDaysAgo: Long) = (0L until 7L).map { today.minusDays(endingDaysAgo + it) }
        /** Total over logged days — null when the window has no water at all. */
        fun windowAvg(endingDaysAgo: Long): Int? {
            val totals = windowDates(endingDaysAgo).map(::waterOn).filter { it > 0 }
            return if (totals.isEmpty()) null else totals.sum() / totals.size
        }

        val avgMlPerDay = windowAvg(0) ?: return HydrationInsights()

        val weekStart = WeekBuckets.weekStart(today)
        val bars = (0L until 7L).map { day ->
            (waterOn(weekStart.plusDays(day)) * 100 / goalMl).coerceIn(0, 100)
        }

        val deltaMlPerDay = windowAvg(7)?.let { avgMlPerDay - it }

        return HydrationInsights(
            hasData = true,
            bars = bars,
            avgMlPerDay = avgMlPerDay,
            deltaMlPerDay = deltaMlPerDay,
            insight = insightFor(avgMlPerDay, deltaMlPerDay, unit),
        )
    }

    /** Never scolds a dip — it reports the number and leaves it there. */
    private fun insightFor(avgMlPerDay: Int, deltaMlPerDay: Int?, unit: HydrationUnit): String {
        val average = "You're averaging ${HydrationFormat.format(avgMlPerDay, unit)} on the days you log"
        val change = when {
            deltaMlPerDay == null -> " — another week of logging and you'll see the change week to week."
            deltaMlPerDay >= 50 -> ", ${deltaMlPerDay}ml more than the previous seven days."
            deltaMlPerDay <= -50 -> ", ${abs(deltaMlPerDay)}ml less than the previous seven days."
            else -> ", holding steady with the previous seven days."
        }
        return average + change
    }
}
