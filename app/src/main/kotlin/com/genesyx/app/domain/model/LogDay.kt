package com.genesyx.app.domain.model

import java.time.LocalDate

/**
 * One day of the user's history: the daily log (if any) plus every pH reading recorded that day.
 * Built by combining the daily-log and pH flows in [com.genesyx.app.ui.history.LogHistoryViewModel].
 */
data class LogDay(
    val date: LocalDate,
    val dailyLog: DailyLog?,
    val phReadings: List<PhReading>,
) {
    /** True when the daily log has any user-entered content (not just an empty/default shell). */
    val hasDailyContent: Boolean = dailyLog != null && with(dailyLog) {
        mood != null || energy != null || symptoms.isNotEmpty() ||
            sleepMinutes != null || supplements.isNotEmpty() || !notes.isNullOrBlank() || waterMl > 0
    }

    /** Nothing worth showing for this day. */
    val isEmpty: Boolean get() = !hasDailyContent && phReadings.isEmpty()
}
