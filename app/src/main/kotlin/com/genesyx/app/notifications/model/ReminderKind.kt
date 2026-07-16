package com.genesyx.app.notifications.model

/**
 * Every reminder the app can schedule, and the fixed facts about each one: which system channel it
 * speaks through, where a tap lands, and a stable request code for its `PendingIntent`.
 *
 * Pure by design — no Android imports — so the scheduling and suppression policy that switches on it
 * stays a plain JVM unit test. The channel ids are plain strings shared with [NotificationChannels].
 */
enum class ReminderKind(
    val channelId: String,
    val deepLink: String,
    val requestCode: Int,
) {
    /** The daily "how was today?" nudge, at a time she picks. */
    DAILY_LOG(Channels.TRACKING, "genesyx://log", 1),

    /** The morning-after "yesterday's still open" nudge, for an active user who slipped one day. */
    MISSED_LOG(Channels.TRACKING, "genesyx://log", 2),

    /** A gentle hydration nudge inside her waking window. */
    HYDRATION(Channels.NUTRITION, "genesyx://track", 3),

    /** Phase-aware nutrition nudge on a cycle-phase transition. Reserved — not scheduled in v1.1. */
    NUTRITION(Channels.NUTRITION, "genesyx://nutrition", 4),

    /** The weekly "your week, at a glance" — informational, silent channel. */
    WEEKLY_INSIGHTS(Channels.INSIGHTS, "genesyx://insights", 5),

    /** The quiet "still here whenever you are" for someone who has been away. */
    REENGAGEMENT(Channels.REENGAGEMENT, "genesyx://home", 6),
    ;

    /** Unique WorkManager work name, so each kind's self-rescheduling chain is independent. */
    val workName: String get() = "reminder-$name"

    /** Channel ids, kept here (pure) so both the enum and the Android channel setup share one source. */
    object Channels {
        const val TRACKING = "tracking_reminders"
        const val NUTRITION = "nutrition_wellness"
        const val INSIGHTS = "weekly_insights"
        const val REENGAGEMENT = "reengagement"
    }
}
