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
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.genesyx.app.MainActivity
import com.genesyx.app.R
import com.genesyx.app.notifications.model.ReminderKind
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * Fires one customer-set supplement reminder, then re-arms it for the next day (a self-rescheduling
 * chain, like [ReminderWorker]). The supplement name shows in the notification but NOT in its public
 * lock-screen version — `VISIBILITY_PRIVATE` with a name-free `publicVersion`, so a passer-by never
 * reads what she takes. Tapping opens Nutrition. Always succeeds — a missed post is not a failure.
 */
class SupplementReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val id = inputData.getString(KEY_ID) ?: return Result.success()
        val name = inputData.getString(KEY_NAME).orEmpty()
        val minutes = inputData.getInt(KEY_MINUTES, -1)
        if (minutes < 0) return Result.success()

        post(id, name)

        // Re-arm tomorrow. Dependencies come through a Hilt EntryPoint because WorkManager builds
        // the worker itself (mirrors ReminderWorker / the sync workers). A reschedule failure is
        // never a delivery failure — the notification already went out.
        runCatching {
            EntryPointAccessors.fromApplication(applicationContext, SupplementReminderEntryPoint::class.java)
                .supplementReminderScheduler()
                .schedule(id, name, minutes)
        }
        return Result.success()
    }

    private fun post(id: String, name: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("genesyx://nutrition"), applicationContext, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pending = PendingIntent.getActivity(
            applicationContext,
            id.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val channel = ReminderKind.Channels.NUTRITION
        // Public (lock-screen) version — deliberately no supplement name.
        val publicVersion = NotificationCompat.Builder(applicationContext, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Supplement reminder")
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        val title = if (name.isBlank()) "Supplement reminder" else "Time for your $name"
        val notification = NotificationCompat.Builder(applicationContext, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText("A gentle reminder to take it.")
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicVersion)
            .build()
        NotificationManagerCompat.from(applicationContext).notify(id.hashCode(), notification)
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface SupplementReminderEntryPoint {
        fun supplementReminderScheduler(): SupplementReminderScheduler
    }

    companion object {
        const val KEY_ID = "id"
        const val KEY_NAME = "name"
        const val KEY_MINUTES = "minutes"
    }
}
