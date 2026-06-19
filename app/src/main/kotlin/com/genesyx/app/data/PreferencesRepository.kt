package com.genesyx.app.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.genesyx.app.domain.model.FocusMode
import com.genesyx.app.domain.model.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App preferences (theme, push toggle, current focus), persisted on-device via DataStore. The theme
 * value drives [com.genesyx.app.ui.AppViewModel] so the Profile dark-mode switch flips the app theme.
 */
@Singleton
class PreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val themeKey = stringPreferencesKey("theme_mode")
    private val pushKey = booleanPreferencesKey("push_enabled")
    private val focusKey = stringPreferencesKey("focus_mode")

    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _pushEnabled = MutableStateFlow(true)
    val pushEnabled: StateFlow<Boolean> = _pushEnabled.asStateFlow()

    private val _focusMode = MutableStateFlow(FocusMode.PREP)
    val focusMode: StateFlow<FocusMode> = _focusMode.asStateFlow()

    init {
        scope.launch {
            val prefs = dataStore.data.first()
            prefs[themeKey]?.let { raw -> runCatching { ThemeMode.valueOf(raw) }.getOrNull()?.let { _themeMode.value = it } }
            prefs[pushKey]?.let { _pushEnabled.value = it }
            prefs[focusKey]?.let { raw -> runCatching { FocusMode.valueOf(raw) }.getOrNull()?.let { _focusMode.value = it } }
        }
    }

    fun setTheme(mode: ThemeMode) {
        _themeMode.value = mode
        scope.launch { dataStore.edit { it[themeKey] = mode.name } }
    }

    fun setPush(enabled: Boolean) {
        _pushEnabled.value = enabled
        scope.launch { dataStore.edit { it[pushKey] = enabled } }
    }

    fun setFocus(mode: FocusMode) {
        _focusMode.value = mode
        scope.launch { dataStore.edit { it[focusKey] = mode.name } }
    }
}
