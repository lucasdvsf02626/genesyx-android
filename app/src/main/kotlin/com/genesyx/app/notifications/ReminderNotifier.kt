package com.genesyx.app.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.genesyx.app.MainActivity
import com.genesyx.app.R
import com.genesyx.app.data.CycleRepository
import com.genesyx.app.data.NotificationSettingsRepository
import com.genesyx.app.data.SessionRepository
import com.genesyx.app.data.local.dao.DailyLogDao
import com.genesyx.app.notifications.model.ReminderKind
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Builds and posts reminders, and owns the runtime side of the suppression decision. The *rules*
 * live in [ReminderPolicy] (pure, tested); this class only gathers the facts those rules need —
 * signed-in state, OS notification state, foreground, whether she's already logged — and, when the
 * policy says yes, posts and advances the engine counters.
 */
@Singleton
class ReminderNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: NotificationSettingsRepository,
    private val session: SessionRepository,
    private val dailyLogDao: DailyLogDao,
    private val cycleRepository: CycleRepository,
) {
    suspend fun postIfAllowed(kind: ReminderKind) {
        val settings = settingsRepository.current()
        val signedIn = session.awaitSignedIn()
        val now = ZonedDateTime.now()
        val today = now.toLocalDate()

        val userId = session.currentUserId()
        // Only a signed-in user is ever eligible (rule 1), so we only pay for the query when it matters.
        val logs = if (signedIn) dailyLogDao.observeAll(userId).first() else emptyList()
        val loggedToday = logs.any { it.date == today }
        val loggedYesterday = logs.any { it.date == today.minusDays(1) }
        val last14 = logs.count { !it.date.isBefore(today.minusDays(13)) }
        val last7 = logs.count { !it.date.isBefore(today.minusDays(6)) }

        val ctx = ReminderPolicy.PostContext(
            signedIn = signedIn,
            notificationsEnabledAtOs = NotificationManagerCompat.from(context).areNotificationsEnabled(),
            appInForeground = AppForegroundTracker.isForeground,
            loggedToday = loggedToday,
            loggedYesterday = loggedYesterday,
            activeUser = last14 >= 3,
            logsInLast7Days = last7,
            hasCycleSettings = cycleRepository.settings.value != null,
            daysSinceLastOpen = settings.lastOpenedEpochDay?.let { today.toEpochDay() - it },
        )

        if (!ReminderPolicy.shouldPost(kind, now, settings, ctx)) return
        if (!post(kind)) return

        settingsRepository.recordPosted(today.toEpochDay())
        when (kind) {
            ReminderKind.REENGAGEMENT -> settingsRepository.recordReengagementPosted(today.toEpochDay())
            ReminderKind.MISSED_LOG -> settingsRepository.recordMissedLog(today.toEpochDay())
            else -> Unit
        }
    }

    /** Returns false (posting nothing) if the runtime permission is absent — belt to the policy's braces. */
    private fun post(kind: ReminderKind): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }

        val copy = ReminderContent.of(kind)
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(kind.deepLink), context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pending = PendingIntent.getActivity(
            context,
            kind.requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notification = NotificationCompat.Builder(context, kind.channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(copy.title)
            .setContentText(copy.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(copy.body))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(kind.requestCode, notification)
        return true
    }
}
