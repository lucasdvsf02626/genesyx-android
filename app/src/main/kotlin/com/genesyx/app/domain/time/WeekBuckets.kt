package com.genesyx.app.domain.time

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * The one definition of "this week" in the app: ISO, Monday-start, Monday..Sunday.
 *
 * It was written out twice before — once in `HydrationInsightLogic` and once privately inside
 * `StreakEngine` — which meant the bars on a card and the streak beneath it could have disagreed
 * about which day a week begins on. Both now come here, so they cannot drift apart.
 */
object WeekBuckets {

    /** The Monday of [date]'s week. [date] itself if it is already a Monday. */
    fun weekStart(date: LocalDate): LocalDate =
        date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    /** The seven dates of [date]'s week, Monday first. */
    fun weekDays(date: LocalDate): List<LocalDate> {
        val start = weekStart(date)
        return (0L until 7L).map { start.plusDays(it) }
    }
}
