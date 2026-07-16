package com.genesyx.app.notifications

/** The states the reminder settings UI reacts to. */
enum class PushPermissionStatus {
    /** API < 33 and notifications are on at the OS level — nothing to request. */
    NOT_REQUIRED,
    GRANTED,
    /** Denied once; the system dialog will show again on the next request. */
    DENIED_SOFT,
    /** Denied twice / "Don't allow" — the system dialog will silently no-op forever. Route to settings. */
    DENIED_PERMANENT,
    /** Permission held (or not needed) but the user switched notifications off in system settings. */
    BLOCKED_IN_SETTINGS,
    NOT_ASKED,
}

/**
 * Turns the four framework signals into a single status the UI can switch on. Pure and unit-tested,
 * because the Android 13 permission model has one genuinely dangerous corner: after a second denial
 * `requestPermission()` does nothing at all, indistinguishable from a first cold call by the
 * framework alone. [hasBeenPrompted] (persisted as `lastPromptedAt`) is what disambiguates them.
 */
object NotificationPermission {

    /** API level at which `POST_NOTIFICATIONS` became a runtime permission. */
    const val RUNTIME_PERMISSION_API = 33

    fun evaluate(
        apiLevel: Int,
        permissionGranted: Boolean,
        notificationsEnabled: Boolean,
        shouldShowRationale: Boolean,
        hasBeenPrompted: Boolean,
    ): PushPermissionStatus {
        if (apiLevel < RUNTIME_PERMISSION_API) {
            // No runtime permission below 33 — the OS master switch is the whole story.
            return if (notificationsEnabled) PushPermissionStatus.NOT_REQUIRED else PushPermissionStatus.BLOCKED_IN_SETTINGS
        }
        return when {
            permissionGranted && notificationsEnabled -> PushPermissionStatus.GRANTED
            permissionGranted && !notificationsEnabled -> PushPermissionStatus.BLOCKED_IN_SETTINGS
            !hasBeenPrompted -> PushPermissionStatus.NOT_ASKED
            shouldShowRationale -> PushPermissionStatus.DENIED_SOFT
            else -> PushPermissionStatus.DENIED_PERMANENT
        }
    }

    /** Only these states can enable reminders directly; the rest must route to system settings. */
    fun canRequestInApp(status: PushPermissionStatus): Boolean =
        status == PushPermissionStatus.NOT_ASKED ||
            status == PushPermissionStatus.DENIED_SOFT ||
            status == PushPermissionStatus.NOT_REQUIRED
}
