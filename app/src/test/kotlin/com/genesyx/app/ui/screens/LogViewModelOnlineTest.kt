package com.genesyx.app.ui.screens

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.genesyx.app.data.DailyLogRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Transitional: locks the v1.0 offline-gate behaviour (FIX 2). Phase 3 replaces isOnline()
 * save-blocking with the WorkManager queue; these cases migrate to "offline write -> PENDING".
 */
class LogViewModelOnlineTest {
    private val repo = mockk<DailyLogRepository>(relaxed = true)
    private val context = mockk<Context>()
    private val cm = mockk<ConnectivityManager>()

    private fun vm(): LogViewModel {
        every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns cm
        return LogViewModel(repo, context)
    }

    @Test
    fun `isOnline true when active network has INTERNET capability`() {
        val net = mockk<Network>()
        val caps = mockk<NetworkCapabilities>()
        every { cm.activeNetwork } returns net
        every { cm.getNetworkCapabilities(net) } returns caps
        every { caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true

        assertTrue(vm().isOnline())
    }

    @Test
    fun `isOnline false when there is no active network`() {
        every { cm.activeNetwork } returns null
        every { cm.getNetworkCapabilities(any()) } returns null

        assertFalse(vm().isOnline())
    }
}
