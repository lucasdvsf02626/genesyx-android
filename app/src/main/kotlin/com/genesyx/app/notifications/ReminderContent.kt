package com.genesyx.app.notifications

import com.genesyx.app.notifications.model.ReminderKind

/**
 * The words each reminder carries. Voice: calm, second-person, never urgent, never shaming, no
 * exclamation marks, no streak-anxiety, no health claims. "Still open," not "you missed." Kept here
 * (pure) so the copy is reviewable in one place and testable without Android.
 */
object ReminderContent {

    data class Copy(val title: String, val body: String)

    fun of(kind: ReminderKind): Copy = when (kind) {
        ReminderKind.DAILY_LOG -> Copy(
            title = "How was today?",
            body = "Take a moment to log how you're feeling.",
        )
        ReminderKind.MISSED_LOG -> Copy(
            title = "Yesterday's still open",
            body = "You can fill in how you felt — it only takes a minute.",
        )
        ReminderKind.HYDRATION -> Copy(
            title = "Water break",
            body = "A glass of water is a small win. Worth taking.",
        )
        ReminderKind.NUTRITION -> Copy(
            title = "Eating for this phase",
            body = "A few foods that tend to help right now.",
        )
        ReminderKind.WEEKLY_INSIGHTS -> Copy(
            title = "Your week, at a glance",
            body = "New patterns from the days you logged.",
        )
        ReminderKind.REENGAGEMENT -> Copy(
            title = "Still here whenever you are",
            body = "No pressure. Your data's exactly where you left it.",
        )
    }
}
