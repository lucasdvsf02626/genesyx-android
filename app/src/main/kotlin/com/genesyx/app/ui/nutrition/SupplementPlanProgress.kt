package com.genesyx.app.ui.nutrition

import com.genesyx.app.domain.content.SupplementPlanItem
import com.genesyx.app.domain.content.supplementPlan

/**
 * Live adherence for the suggested-plan card: how much of the plan today's log already covers.
 *
 * Matches stored names the way [com.genesyx.app.ui.insights.UserSupplementInsightLogic] does —
 * trimmed, case-insensitive — so a value written by an older build or the other platform still
 * counts. Anything logged outside the plan (Iron, her own supplements) is simply not scored,
 * mirroring [com.genesyx.app.ui.insights.SupplementInsightLogic]'s denominator rule.
 */
object SupplementPlanProgress {

    /** How many distinct plan items appear in today's logged supplement names. */
    fun takenToday(
        loggedNames: Collection<String>,
        plan: List<SupplementPlanItem> = supplementPlan,
    ): Int {
        val logged = loggedNames.map { it.trim() }.filter { it.isNotEmpty() }
        return plan.count { item ->
            logged.any { it.equals(item.supplement.wireName, ignoreCase = true) }
        }
    }

    /** iOS's plan-card line, verbatim: "None logged yet today" / "N of M taken today". */
    fun statusLine(taken: Int, planSize: Int = supplementPlan.size): String =
        if (taken == 0) "None logged yet today" else "$taken of $planSize taken today"
}
