package com.genesyx.app.data.local.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.genesyx.app.domain.model.FocusMode
import com.genesyx.app.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Typed wrapper over the app's [DataStore]. Persists preferences (theme/push/focus/onboarding) and
 * the local session mirror (signed-in, userId, email, display name) so they survive process death.
 */
@Singleton
class GenesyxPreferencesDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    private object Keys {
        val THEME = stringPreferencesKey("theme_mode")
        val PUSH = booleanPreferencesKey("push_enabled")
        val FOCUS = stringPreferencesKey("focus_mode")
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val SIGNED_IN = booleanPreferencesKey("signed_in")
        val USER_ID = stringPreferencesKey("user_id")
        val EMAIL = stringPreferencesKey("email")
        val DISPLAY_NAME = stringPreferencesKey("display_name")
    }

    // Default LIGHT: a fresh install (or an upgrade with no stored choice) opens in light, so the
    // app never starts locked in dark on a dark-set device. Users can pick System/Light/Dark.
    val themeMode: Flow<ThemeMode> = dataStore.data.map { p ->
        p[Keys.THEME]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.LIGHT
    }
    val pushEnabled: Flow<Boolean> = dataStore.data.map { it[Keys.PUSH] ?: true }
    val focusMode: Flow<FocusMode> = dataStore.data.map { p ->
        p[Keys.FOCUS]?.let { runCatching { FocusMode.valueOf(it) }.getOrNull() } ?: FocusMode.PREP
    }
    val onboardingComplete: Flow<Boolean> = dataStore.data.map { it[Keys.ONBOARDING_COMPLETE] ?: false }

    val signedIn: Flow<Boolean> = dataStore.data.map { it[Keys.SIGNED_IN] ?: false }
    val userId: Flow<String?> = dataStore.data.map { it[Keys.USER_ID] }
    val email: Flow<String?> = dataStore.data.map { it[Keys.EMAIL] }
    val displayName: Flow<String?> = dataStore.data.map { it[Keys.DISPLAY_NAME] }

    suspend fun setTheme(mode: ThemeMode) = dataStore.edit { it[Keys.THEME] = mode.name }.let {}
    suspend fun setPush(enabled: Boolean) = dataStore.edit { it[Keys.PUSH] = enabled }.let {}
    suspend fun setFocus(mode: FocusMode) = dataStore.edit { it[Keys.FOCUS] = mode.name }.let {}
    suspend fun setOnboardingComplete(v: Boolean) = dataStore.edit { it[Keys.ONBOARDING_COMPLETE] = v }.let {}

    suspend fun setSession(userId: String, email: String?, displayName: String?) {
        dataStore.edit {
            it[Keys.SIGNED_IN] = true
            it[Keys.USER_ID] = userId
            if (email != null) it[Keys.EMAIL] = email else it.remove(Keys.EMAIL)
            if (displayName != null) it[Keys.DISPLAY_NAME] = displayName else it.remove(Keys.DISPLAY_NAME)
        }
    }

    suspend fun setDisplayName(name: String) = dataStore.edit { it[Keys.DISPLAY_NAME] = name }.let {}

    suspend fun clearSession() {
        dataStore.edit {
            it[Keys.SIGNED_IN] = false
            it.remove(Keys.USER_ID)
            it.remove(Keys.EMAIL)
            it.remove(Keys.DISPLAY_NAME)
        }
    }
}
