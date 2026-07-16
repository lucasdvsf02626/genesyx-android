package com.genesyx.app.notifications

import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationPermissionTest {

    private fun eval(
        api: Int,
        granted: Boolean,
        enabled: Boolean,
        rationale: Boolean,
        prompted: Boolean,
    ) = NotificationPermission.evaluate(api, granted, enabled, rationale, prompted)

    @Test
    fun `below API 33 needs no runtime permission when notifications are on`() {
        assertEquals(
            PushPermissionStatus.NOT_REQUIRED,
            eval(api = 30, granted = false, enabled = true, rationale = false, prompted = false),
        )
    }

    @Test
    fun `below API 33 with notifications off is blocked in settings`() {
        assertEquals(
            PushPermissionStatus.BLOCKED_IN_SETTINGS,
            eval(api = 30, granted = false, enabled = false, rationale = false, prompted = false),
        )
    }

    @Test
    fun `granted and enabled is granted`() {
        assertEquals(
            PushPermissionStatus.GRANTED,
            eval(api = 34, granted = true, enabled = true, rationale = false, prompted = true),
        )
    }

    @Test
    fun `granted but switched off in settings is blocked`() {
        assertEquals(
            PushPermissionStatus.BLOCKED_IN_SETTINGS,
            eval(api = 34, granted = true, enabled = false, rationale = false, prompted = true),
        )
    }

    @Test
    fun `never asked is distinguishable from permanent denial`() {
        assertEquals(
            PushPermissionStatus.NOT_ASKED,
            eval(api = 34, granted = false, enabled = true, rationale = false, prompted = false),
        )
    }

    @Test
    fun `a soft denial still shows the rationale`() {
        assertEquals(
            PushPermissionStatus.DENIED_SOFT,
            eval(api = 34, granted = false, enabled = true, rationale = true, prompted = true),
        )
    }

    @Test
    fun `prompted, no rationale, still denied is permanent`() {
        // The dangerous corner: framework can't tell this from a cold call — lastPromptedAt can.
        assertEquals(
            PushPermissionStatus.DENIED_PERMANENT,
            eval(api = 34, granted = false, enabled = true, rationale = false, prompted = true),
        )
    }

    @Test
    fun `only asked, soft-denied and not-required states may request in-app`() {
        assertEquals(true, NotificationPermission.canRequestInApp(PushPermissionStatus.NOT_ASKED))
        assertEquals(true, NotificationPermission.canRequestInApp(PushPermissionStatus.DENIED_SOFT))
        assertEquals(false, NotificationPermission.canRequestInApp(PushPermissionStatus.DENIED_PERMANENT))
        assertEquals(false, NotificationPermission.canRequestInApp(PushPermissionStatus.BLOCKED_IN_SETTINGS))
    }
}
