package com.genesyx.app.notifications

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * On-device proof that a customer-set supplement reminder actually fires: it runs the real
 * [SupplementReminderWorker] and asserts the notification lands in the system [NotificationManager],
 * with the private (lock-screen) copy carrying no supplement name.
 */
@RunWith(AndroidJUnit4::class)
class SupplementReminderWorkerInstrumentedTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val notificationManager: NotificationManager
        get() = context.getSystemService(NotificationManager::class.java)

    @Before
    fun setup() {
        NotificationChannels.createAll(context)
        InstrumentationRegistry.getInstrumentation().uiAutomation
            .grantRuntimePermission(context.packageName, "android.permission.POST_NOTIFICATIONS")
        notificationManager.cancelAll()
    }

    @After
    fun teardown() {
        notificationManager.cancelAll()
    }

    @Test
    fun worker_posts_a_named_notification_with_a_nameless_public_version() = runBlocking {
        val id = "supp-test-id"
        val worker = TestListenableWorkerBuilder<SupplementReminderWorker>(context)
            .setInputData(
                workDataOf(
                    SupplementReminderWorker.KEY_ID to id,
                    SupplementReminderWorker.KEY_NAME to "Magnesium",
                    SupplementReminderWorker.KEY_MINUTES to 9 * 60,
                ),
            )
            .build()

        val result = worker.doWork()
        assertTrue(result is ListenableWorker.Result.Success)

        withTimeout(5_000) {
            while (notificationManager.activeNotifications.none { it.id == id.hashCode() }) delay(50)
        }
        val posted = notificationManager.activeNotifications.first { it.id == id.hashCode() }
        assertEquals("nutrition_wellness", posted.notification.channelId)
        assertEquals(
            "Time for your Magnesium",
            posted.notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString(),
        )
        // The lock-screen (public) version must NOT leak the supplement name.
        val public = posted.notification.publicVersion
        assertTrue("expected a nameless public version", public != null)
        assertEquals(
            "Supplement reminder",
            public!!.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString(),
        )
    }
}
