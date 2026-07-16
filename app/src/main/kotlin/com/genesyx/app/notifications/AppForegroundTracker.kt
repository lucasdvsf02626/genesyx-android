package com.genesyx.app.notifications

import java.util.concurrent.atomic.AtomicBoolean

/**
 * Whether the app is currently in the foreground. Genesyx is single-activity, so [MainActivity]'s
 * start/stop is the process's foreground state. The reminder worker runs in the same process and
 * reads this to obey rule "app foregrounded → suppress": posting "log your day" over the Log screen
 * is absurd. An [AtomicBoolean] instead of a lifecycle-process dependency we'd otherwise add for one
 * flag.
 */
object AppForegroundTracker {
    private val foreground = AtomicBoolean(false)

    val isForeground: Boolean get() = foreground.get()

    fun onEnterForeground() = foreground.set(true)
    fun onLeaveForeground() = foreground.set(false)
}
