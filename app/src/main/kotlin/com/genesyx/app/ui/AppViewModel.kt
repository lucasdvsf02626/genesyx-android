package com.genesyx.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.genesyx.app.core.di.ApplicationScope
import com.genesyx.app.data.PreferencesRepository
import com.genesyx.app.data.ProfileRepository
import com.genesyx.app.data.SessionRepository
import kotlinx.coroutines.CoroutineScope
import com.genesyx.app.domain.model.ThemeMode
import com.genesyx.app.ui.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** App-level state: exposes the theme preference so MainActivity can pick light/dark/system, and
 *  resolves the launch start destination so a signed-in user skips onboarding straight to Home. */
@HiltViewModel
class AppViewModel @Inject constructor(
    preferencesRepository: PreferencesRepository,
    session: SessionRepository,
    private val profileRepository: ProfileRepository,
    @ApplicationScope private val appScope: CoroutineScope,
) : ViewModel() {
    val themeMode: StateFlow<ThemeMode> = preferencesRepository.themeMode

    /** null = session not yet resolved (hold the splash); otherwise the route to start on. */
    private val _startRoute = MutableStateFlow<String?>(null)
    val startRoute: StateFlow<String?> = _startRoute.asStateFlow()

    init {
        viewModelScope.launch {
            val signedIn = session.awaitSignedIn()
            _startRoute.value = if (signedIn) Screen.Home.route else Screen.Splash.route
            // Sign-in was the only thing that ever pulled the profile row, so an account that was
            // already signed in kept greeting her by the guess made from her address. This also
            // gives an offline rename its retry — refresh pushes what's owed before it pulls.
            // Off the ViewModel's scope: it must not be cancelled by the splash going away.
            if (signedIn) appScope.launch { profileRepository.refresh(session.currentUserId()) }
        }
    }
}
