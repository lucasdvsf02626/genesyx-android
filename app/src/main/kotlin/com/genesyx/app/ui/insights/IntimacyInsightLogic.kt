package com.genesyx.app.ui.insights

import com.genesyx.app.domain.model.DailyLog
import com.genesyx.app.domain.time.WeekBuckets
import java.time.LocalDate

data class IntimacyInsights(
    /** True once she has ever recorded intimacy — the card stays hidden until then, so it never
     *  appears to someone who does not use the field. */
    val hasData: Boolean = false,
    /** Mon..Sun of the current week — true on days intimacy was recorded. */
    val perDay: List<Boolean> = List(7) { false },
    val daysThisWeek: Int = 0,
    val insight: String = "",
)

/**
 * Intimacy for the current week, from the private [DailyLog.sexualActivity] flag. Read-only and
 * **private to her** — the same field the log toggle writes, never shared with any partner feature.
 * The copy states what she recorded and nothing more: no timing advice, no fertility framing, no
 * judgement. Hidden entirely until she has recorded it at least once.
 */
object IntimacyInsightLogic {

    fun compute(
        logsByDate: Map<LocalDate, DailyLog>,
        today: LocalDate = LocalDate.now(),
    ): IntimacyInsights {
        val everRecorded = logsByDate.values.any { it.sexualActivity == true }
        if (!everRecorded) return IntimacyInsights()

        val perDay = WeekBuckets.weekDays(today).map { logsByDate[it]?.sexualActivity == true }
        val days = perDay.count { it }
        return IntimacyInsights(
            hasData = true,
            perDay = perDay,
            daysThisWeek = days,
            insight = when (days) {
                0 -> "Nothing recorded this week. This stays private to you."
                1 -> "Recorded on 1 day this week."
                else -> "Recorded on $days days this week."
            },
        )
    }
}
