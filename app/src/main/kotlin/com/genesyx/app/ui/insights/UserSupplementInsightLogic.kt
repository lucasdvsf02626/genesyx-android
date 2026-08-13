package com.genesyx.app.ui.insights

import com.genesyx.app.domain.model.DailyLog
import com.genesyx.app.domain.time.WeekBuckets
import java.time.LocalDate

/** One of the user's own supplements and how her week went with it. */
data class UserSupplementWeek(
    val name: String,
    /** Mon..Sun of the current week — true on days this supplement appears in that day's log. */
    val perDay: List<Boolean>,
    val daysLogged: Int,
)

data class UserSupplementInsights(
    /** False when she has no "Your supplements" entries — the section is then hidden entirely. */
    val hasSupplements: Boolean = false,
    val rows: List<UserSupplementWeek> = emptyList(),
)

/**
 * Per-supplement weekly log counts for the user's OWN supplements ("Your supplements" in Nutrition),
 * matched by name (case-insensitive) against what each day's log stored. Distinct from
 * [SupplementInsightLogic], which scores only the fixed starter plan — this simply surfaces how often
 * she logged each of her own, so the things she actually tracks show up in Insights too. A supplement
 * she hasn't logged this week still lists (0/7): it is hers, so it is shown, not hidden.
 */
object UserSupplementInsightLogic {

    fun compute(
        supplementNames: List<String>,
        logsByDate: Map<LocalDate, DailyLog>,
        today: LocalDate = LocalDate.now(),
    ): UserSupplementInsights {
        val names = supplementNames
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinctBy { it.lowercase() }
        if (names.isEmpty()) return UserSupplementInsights()

        val week = WeekBuckets.weekDays(today)
        val rows = names.map { name ->
            val perDay = week.map { date ->
                logsByDate[date]?.supplements.orEmpty().any { it.equals(name, ignoreCase = true) }
            }
            UserSupplementWeek(name = name, perDay = perDay, daysLogged = perDay.count { it })
        }
        return UserSupplementInsights(hasSupplements = true, rows = rows)
    }
}
