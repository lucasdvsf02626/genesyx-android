package com.genesyx.app.notifications

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.genesyx.app.data.NotificationSettingsRepository
import com.genesyx.app.data.SessionRepository
import com.genesyx.app.notifications.model.ReminderKind
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalTime
import javax.inject.Inject

/**
 * On-device end-to-end proof that a reminder actually fires: it drives the real [ReminderNotifier]
 * through the real [ReminderPolicy] and asserts the notification lands in the system
 * [NotificationManager], exercising channel creation, the post path, and `NotificationManagerCompat.notify`.
 *
 * It does the two things a background worker would rely on being true — the user is signed in, and the
 * clock isn't inside quiet hours — then posts, exactly as the scheduled `ReminderWorker` would.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ReminderNotifierInstrumentedTest {

    @get:Rule val hilt = HiltAndroidRule(this)

    @Inject lateinit var notifier: ReminderNotifier
    @Inject lateinit var session: SessionRepository
    @Inject lateinit var settings: NotificationSettingsRepository

    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val notificationManager: NotificationManager
        get() = context.getSystemService(NotificationManager::class.java)

    @Before
    fun setup() {
        hilt.inject()
        // HiltTestApplication doesn't run GenesyxApplication.onCreate, so create the channels here —
        // a notification to a missing channel is silently dropped on API 26+.
        NotificationChannels.createAll(context)
        // API 33+ runtime permission, granted so the real post path isn't gated.
        InstrumentationRegistry.getInstrumentation().uiAutomation
            .grantRuntimePermission(context.packageName, "android.permission.POST_NOTIFICATIONS")
        notificationManager.cancelAll()
    }

    @After
    fun teardown() = runBlocking {
        notificationManager.cancelAll()
        session.signOut()
    }

    @Test
    fun daily_log_reminder_posts_a_real_notification() = runBlocking {
        session.signIn(email = "reminder-test@genesyx.app", name = "Test", userId = "reminder-test-user")
        withTimeout(5_000) { while (!session.awaitSignedIn()) delay(50) }

        // The emulator clock may sit inside the default quiet window (22:00–07:00) — take it out.
        settings.setQuietHours(enabled = false, start = LocalTime.of(22, 0), end = LocalTime.of(7, 0))
        settings.setKindEnabled(ReminderKind.DAILY_LOG, true)

        notifier.postIfAllowed(ReminderKind.DAILY_LOG)

        withTimeout(5_000) {
            while (notificationManager.activeNotifications.none { it.id == ReminderKind.DAILY_LOG.requestCode }) {
                delay(50)
            }
        }

        val posted = notificationManager.activeNotifications.first { it.id == ReminderKind.DAILY_LOG.requestCode }
        assertEquals("tracking_reminders", posted.notification.channelId)
        assertEquals(
            "How was today?",
            posted.notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString(),
        )
        assertEquals(
            "Take a moment to log how you're feeling.",
            posted.notification.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString(),
        )
    }
}
