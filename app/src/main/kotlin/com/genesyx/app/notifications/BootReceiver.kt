package com.genesyx.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.genesyx.app.data.NotificationSettingsRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Re-arms the reminder chains after a reboot. WorkManager already persists across reboot, so this is
 * belt-and-braces against OEM task-killers that drop pending work — cheap insurance, not the primary
 * mechanism.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        val entry = EntryPointAccessors.fromApplication(context.applicationContext, BootEntryPoint::class.java)
        CoroutineScope(Dispatchers.Default).launch {
            try {
                entry.scheduler().rescheduleAll(entry.settingsRepository().current())
            } finally {
                pending.finish()
            }
        }
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface BootEntryPoint {
        fun scheduler(): ReminderScheduler
        fun settingsRepository(): NotificationSettingsRepository
    }
}
