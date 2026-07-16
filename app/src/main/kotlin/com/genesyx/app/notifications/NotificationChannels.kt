package com.genesyx.app.notifications

import android.content.Context
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import com.genesyx.app.R
import com.genesyx.app.notifications.model.ReminderKind

/**
 * The four notification channels, created unconditionally in [com.genesyx.app.GenesyxApplication].
 * `minSdk = 26` means channels exist on every supported device, so there is no version guard;
 * re-creating an existing channel is a no-op, so calling this on every launch is safe.
 *
 * Deliberately four, not one-per-kind: users manage channels in system settings and six switches is a
 * wall. Re-engagement is its own low-importance, badge-less channel precisely so a user who resents
 * it can silence that one thing instead of turning off every reminder — or uninstalling.
 */
object NotificationChannels {

    fun createAll(context: Context) {
        val manager = NotificationManagerCompat.from(context)
        manager.createNotificationChannelsCompat(
            listOf(
                channel(
                    context,
                    ReminderKind.Channels.TRACKING,
                    R.string.channel_tracking_name,
                    R.string.channel_tracking_desc,
                    NotificationManagerCompat.IMPORTANCE_DEFAULT,
                ),
                channel(
                    context,
                    ReminderKind.Channels.NUTRITION,
                    R.string.channel_nutrition_name,
                    R.string.channel_nutrition_desc,
                    NotificationManagerCompat.IMPORTANCE_DEFAULT,
                ),
                channel(
                    context,
                    ReminderKind.Channels.INSIGHTS,
                    R.string.channel_insights_name,
                    R.string.channel_insights_desc,
                    NotificationManagerCompat.IMPORTANCE_LOW,
                    badge = false,
                ),
                channel(
                    context,
                    ReminderKind.Channels.REENGAGEMENT,
                    R.string.channel_reengagement_name,
                    R.string.channel_reengagement_desc,
                    NotificationManagerCompat.IMPORTANCE_LOW,
                    badge = false,
                ),
            ),
        )
    }

    private fun channel(
        context: Context,
        id: String,
        nameRes: Int,
        descRes: Int,
        importance: Int,
        badge: Boolean = true,
    ): NotificationChannelCompat =
        NotificationChannelCompat.Builder(id, importance)
            .setName(context.getString(nameRes))
            .setDescription(context.getString(descRes))
            .setShowBadge(badge)
            .build()
}
