package com.genesyx.app

import android.app.Application
import com.genesyx.app.notifications.NotificationChannels
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class GenesyxApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Channels exist on every supported device (minSdk 26); re-creating is a no-op, so this is
        // safe on every launch and is the one place channels are defined.
        NotificationChannels.createAll(this)
    }
}
