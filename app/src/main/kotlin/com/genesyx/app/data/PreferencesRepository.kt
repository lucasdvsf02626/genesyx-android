package com.genesyx.app.data

import com.genesyx.app.core.di.ApplicationScope
import com.genesyx.app.data.local.datastore.GenesyxPreferencesDataStore
import com.genesyx.app.domain.model.FocusMode
import com.genesyx.app.domain.model.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App preferences (theme, push toggle, current focus, onboarding-complete), now persisted via
 * DataStore so they survive process death. Public API is unchanged (StateFlows + setters) — the
 * theme value still drives [com.genesyx.app.ui.AppViewModel] so the Profile dark-mode switch flips
 * the app theme live, and now the choice is remembered across restarts.
 */
@Singleton
class PreferencesRepository @Inject constructor(
    private val store: GenesyxPreferencesDataStore,
    @ApplicationScope private val scope: CoroutineScope,
) {
    val themeMode: StateFlow<ThemeMode> =
        store.themeMode.stateIn(scope, SharingStarted.Eagerly, ThemeMode.SYSTEM)
    val pushEnabled: StateFlow<Boolean> =
        store.pushEnabled.stateIn(scope, SharingStarted.Eagerly, true)
    val focusMode: StateFlow<FocusMode> =
        store.focusMode.stateIn(scope, SharingStarted.Eagerly, FocusMode.PREP)
    val onboardingComplete: StateFlow<Boolean> =
        store.onboardingComplete.stateIn(scope, SharingStarted.Eagerly, false)

    fun setTheme(mode: ThemeMode) { scope.launch { store.setTheme(mode) } }
    fun setPush(enabled: Boolean) { scope.launch { store.setPush(enabled) } }
    fun setFocus(mode: FocusMode) { scope.launch { store.setFocus(mode) } }
    fun setOnboardingComplete(complete: Boolean) { scope.launch { store.setOnboardingComplete(complete) } }
}
