package com.genesyx.app.data

import com.genesyx.app.domain.model.FocusMode
import com.genesyx.app.domain.model.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory app preferences (theme, push toggle, current focus). The theme value drives
 * [com.genesyx.app.ui.AppViewModel] so the Profile dark-mode switch flips the app theme live.
 */
@Singleton
class PreferencesRepository @Inject constructor() {

    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _pushEnabled = MutableStateFlow(true)
    val pushEnabled: StateFlow<Boolean> = _pushEnabled.asStateFlow()

    private val _focusMode = MutableStateFlow(FocusMode.PREP)
    val focusMode: StateFlow<FocusMode> = _focusMode.asStateFlow()

    fun setTheme(mode: ThemeMode) { _themeMode.value = mode }
    fun setPush(enabled: Boolean) { _pushEnabled.value = enabled }
    fun setFocus(mode: FocusMode) { _focusMode.value = mode }
}
