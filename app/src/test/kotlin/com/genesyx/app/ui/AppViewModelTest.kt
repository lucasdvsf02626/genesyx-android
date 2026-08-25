package com.genesyx.app.ui

import com.genesyx.app.data.PreferencesRepository
import com.genesyx.app.data.ProfileRepository
import com.genesyx.app.data.SessionRepository
import com.genesyx.app.domain.model.ThemeMode
import com.genesyx.app.ui.navigation.Screen
import com.genesyx.app.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
    private val session = mockk<SessionRepository> {
        every { currentUserId() } returns "u1"
    }
    private val profile = mockk<ProfileRepository>(relaxed = true)

    private fun viewModel(scope: CoroutineScope) = AppViewModel(prefs, session, profile, scope)

    @Test
    fun `signed-in user starts on Home`() = runTest {
        coEvery { session.awaitSignedIn() } returns true
        val vm = viewModel(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        assertEquals(Screen.Home.route, vm.startRoute.value)
    }

    @Test
    fun `signed-out user starts on Splash`() = runTest {
        coEvery { session.awaitSignedIn() } returns false
        val vm = viewModel(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        assertEquals(Screen.Splash.route, vm.startRoute.value)
    }

    /**
     * Sign-in used to be the only thing that pulled the profile row, so an account already signed
     * in went on being greeted by the guess made from her email address. Launch is the retry.
     */
    @Test
    fun `launching signed in pulls the profile`() = runTest {
        coEvery { session.awaitSignedIn() } returns true
        viewModel(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        coVerify(exactly = 1) { profile.refresh(any()) }
    }

    @Test
    fun `launching signed out touches no profile row`() = runTest {
        coEvery { session.awaitSignedIn() } returns false
        viewModel(CoroutineScope(UnconfinedTestDispatcher(testScheduler)))
        coVerify(exactly = 0) { profile.refresh(any()) }
    }
}
