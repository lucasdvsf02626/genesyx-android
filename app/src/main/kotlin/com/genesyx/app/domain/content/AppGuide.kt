package com.genesyx.app.domain.content

/**
 * The index behind "How to use Genesyx" and the in-tab help links.
 *
 * Every [slug] must be an always-published article (`publishedAt == null`). A withheld guide
 * would render as a row that opens "unavailable" — failing nothing and looking fine in review.
 * `AppGuideTest` enforces this.
 */
data class AppGuideEntry(
    val tab: String,
    val slug: String,
    val purpose: String,
)

object AppGuide {
    val entries: List<AppGuideEntry> = listOf(
        AppGuideEntry("Home", "getting-started-first-week", "What to expect in your first seven days"),
        AppGuideEntry("Home", "guide-how-hydration-works", "How your water target is set, and why it is not eight glasses"),
        AppGuideEntry("Track", "guide-how-the-log-works", "Recording a day, and what each entry is for"),
        AppGuideEntry("Track", "guide-cycle-and-phases", "How your phases are worked out, and what they change"),
        AppGuideEntry("Track", "guide-logging-symptoms", "Noting how you feel, and why it is worth doing"),
        AppGuideEntry("Track", "guide-sleep-tracking", "Logging sleep, and what it feeds into"),
        AppGuideEntry("pH", "guide-vaginal-ph-tracker", "What the tracker is for"),
        AppGuideEntry("pH", "guide-how-to-log-ph", "Taking a reading you can trust"),
        AppGuideEntry("pH", "guide-understanding-vaginal-ph", "What the number means"),
        AppGuideEntry("pH", "guide-track-ph-in-nutrition", "Reading the trend rather than a single result"),
        AppGuideEntry("Nutrition", "guide-nutrition-focus", "How your focus foods are chosen"),
        AppGuideEntry("Insights", "reading-your-trends", "Reading your patterns without over-reading them"),
    )

    const val HOME = "getting-started-first-week"
    const val TRACK = "guide-how-the-log-works"
    const val PH = "guide-understanding-vaginal-ph"
    const val NUTRITION = "guide-nutrition-focus"
    const val INSIGHTS = "reading-your-trends"

    const val HOME_LABEL = "New here? What your first week looks like →"
    const val TRACK_LABEL = "How the log works, and what each entry is for →"
    const val PH_LABEL = "Read: Understanding your vaginal pH →"
    const val NUTRITION_LABEL = "How your focus foods are chosen →"
    const val INSIGHTS_LABEL = "Reading your trends without over-reading them →"

    val tabSignposts: List<String> = listOf(HOME, TRACK, PH, NUTRITION, INSIGHTS)

    val byTab: List<Pair<String, List<AppGuideEntry>>>
        get() {
            val order = mutableListOf<String>()
            val grouped = linkedMapOf<String, MutableList<AppGuideEntry>>()
            for (entry in entries) {
                if (entry.tab !in grouped) order.add(entry.tab)
                grouped.getOrPut(entry.tab) { mutableListOf() }.add(entry)
            }
            return order.map { it to (grouped[it] ?: emptyList()) }
        }
}

/** Hub routes that must never collide with an article slug. */
object LearnHubRoutes {
    const val HOW_TO_USE = "how-to-use-genesyx"
    const val TWELVE_WEEK = "twelve-week-plan"
}
