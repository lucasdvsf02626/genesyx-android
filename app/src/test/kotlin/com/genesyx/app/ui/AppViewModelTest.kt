package com.genesyx.app.ui

import com.genesyx.app.data.PreferencesRepository
import com.genesyx.app.data.SessionRepository
import com.genesyx.app.domain.model.ThemeMode
import com.genesyx.app.ui.navigation.Screen
import com.genesyx.app.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {
    @get:Rule val main = MainDispatcherRule()   // Unconfined main → init launch runs eagerly

    private val prefs = mockk<PreferencesRepository> {
        every { themeMode } returns MutableStateFlow(ThemeMode.SYSTEM)
    }
    private val session = mockk<SessionRepository>()

    @Test
    fun `signed-in user starts on Home`() = runTest {
        coEvery { session.awaitSignedIn() } returns true
        assertEquals(Screen.Home.route, AppViewModel(prefs, session).startRoute.value)
    }

    @Test
    fun `signed-out user starts on Splash`() = runTest {
        coEvery { session.awaitSignedIn() } returns false
        assertEquals(Screen.Splash.route, AppViewModel(prefs, session).startRoute.value)
    }
}
