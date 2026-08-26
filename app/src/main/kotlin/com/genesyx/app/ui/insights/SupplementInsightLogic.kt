package com.genesyx.app.ui.insights

import com.genesyx.app.domain.model.DailyLog
import com.genesyx.app.domain.model.Supplement
import com.genesyx.app.domain.model.SupplementToggleSet
import com.genesyx.app.domain.model.UserSupplement
import com.genesyx.app.domain.time.WeekBuckets
import java.time.LocalDate

/**
 * Pure supplement-adherence computation for the current Mon–Sun week.
 *
 * The denominator is her toggle set — the bundled plan plus her own entries, exactly the chips
 * the Nutrition tab shows ([SupplementToggleSet]) — so the card's "N of M" is the Nutrition
 * card's "N of M". Iron is loggable but outside the plan, so taking it neither pushes a bar past
 * 100 nor counts towards adherence — it is recorded, not scored, and skipping it is not a miss.
 *
 * Stored strings are matched the way [Supplement.fromWire] matches — trimmed, case-insensitive.
 * Anything unrecognised — an older build, or a value another client wrote — simply does not
 * score, rather than being guessed at.
 */
object SupplementInsightLogic {

    fun compute(
        logsByDate: Map<LocalDate, DailyLog>,
        today: LocalDate = LocalDate.now(),
        plan: List<Supplement> = Supplement.defaultPlan,
        custom: List<UserSupplement> = emptyList(),
    ): SupplementInsights {
        val entries = SupplementToggleSet.build(custom, plan)
        if (entries.isEmpty()) return SupplementInsights(hasPlan = false)

        val todayLogged = logsByDate[today]?.supplements.orEmpty()
        val todayItems = entries.map { SupplementTodayItem(it.display, SupplementToggleSet.isLogged(it, todayLogged)) }
        val todayTaken = todayItems.count { it.logged }

        val takenPerDay = WeekBuckets.weekDays(today).map { date ->
            SupplementToggleSet.takenCount(entries, logsByDate[date]?.supplements.orEmpty())
        }

        val suppTotal = takenPerDay.sum()
        if (suppTotal == 0) return SupplementInsights(planSize = entries.size, todayItems = todayItems)

        val daysLogged = takenPerDay.count { it > 0 }
        return SupplementInsights(
            hasData = true,
            bars = takenPerDay.map { it * 100 / entries.size },
            daysLogged = daysLogged,
            suppTotal = suppTotal,
            planSize = entries.size,
            todayTaken = todayTaken,
            todayItems = todayItems,
            insight = insightFor(daysLogged, takenPerDay.count { it == entries.size }),
        )
    }

    /** Present tense, and never names a day she didn't reach. What she did is the whole message. */
    private fun insightFor(daysLogged: Int, fullDays: Int): String = when {
        daysLogged < 2 -> "Early days this week — your pattern builds as you log."
        fullDays >= 6 -> "Your whole plan on $fullDays days this week. Beautifully steady."
        fullDays >= 1 -> "Your whole plan on $fullDays of the $daysLogged days you've logged this week."
        else -> "Something from your plan on $daysLogged days this week."
    }
}
