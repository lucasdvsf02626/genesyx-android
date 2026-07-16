package com.genesyx.app.ui.insights

import com.genesyx.app.domain.model.DailyLog
import com.genesyx.app.domain.model.Supplement
import com.genesyx.app.domain.time.WeekBuckets
import java.time.LocalDate

/**
 * Pure supplement-adherence computation for the current Mon–Sun week.
 *
 * The denominator is her plan, not the number of things she could tick. Iron is loggable but sits
 * outside the default plan, so taking it neither pushes a bar past 100 nor counts towards adherence
 * — it is recorded, not scored, and skipping it is not a miss.
 *
 * Stored strings come back through [Supplement.fromWire]. Anything unrecognised — an older build, or
 * a value another client wrote — simply does not score, rather than being guessed at.
 */
object SupplementInsightLogic {

    fun compute(
        logsByDate: Map<LocalDate, DailyLog>,
        today: LocalDate = LocalDate.now(),
        plan: List<Supplement> = Supplement.defaultPlan,
    ): SupplementInsights {
        if (plan.isEmpty()) return SupplementInsights(hasPlan = false)

        val takenPerDay = WeekBuckets.weekDays(today).map { date ->
            val logged = logsByDate[date]?.supplements.orEmpty().mapNotNull(Supplement::fromWire)
            plan.count { it in logged }
        }

        val suppTotal = takenPerDay.sum()
        if (suppTotal == 0) return SupplementInsights()

        val daysLogged = takenPerDay.count { it > 0 }
        return SupplementInsights(
            hasData = true,
            bars = takenPerDay.map { it * 100 / plan.size },
            daysLogged = daysLogged,
            suppTotal = suppTotal,
            planSize = plan.size,
            insight = insightFor(daysLogged, takenPerDay.count { it == plan.size }),
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
