package com.genesyx.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "genesyx_prefs")

@Singleton
class OnboardingPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val store get() = context.dataStore

    private object Keys {
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val CYCLE_LENGTH = intPreferencesKey("cycle_length")
        val PERIOD_LENGTH = intPreferencesKey("period_length")
        val LAST_PERIOD_DATE = stringPreferencesKey("last_period_date")
        val USER_NAME = stringPreferencesKey("user_name")
        val THEME = stringPreferencesKey("theme")
    }

    val onboardingCompleted: Flow<Boolean> = store.data.map { it[Keys.ONBOARDING_DONE] ?: false }
    val cycleLength: Flow<Int> = store.data.map { it[Keys.CYCLE_LENGTH] ?: 28 }
    val periodLength: Flow<Int> = store.data.map { it[Keys.PERIOD_LENGTH] ?: 5 }
    val lastPeriodDate: Flow<String?> = store.data.map { it[Keys.LAST_PERIOD_DATE] }
    val userName: Flow<String> = store.data.map { it[Keys.USER_NAME] ?: "" }
    val theme: Flow<String> = store.data.map { it[Keys.THEME] ?: "dark" }

    suspend fun setOnboardingCompleted() = store.edit { it[Keys.ONBOARDING_DONE] = true }

    suspend fun saveCyclePrefs(cycleLength: Int, periodLength: Int, lastPeriodDate: String) =
        store.edit {
            it[Keys.CYCLE_LENGTH] = cycleLength
            it[Keys.PERIOD_LENGTH] = periodLength
            it[Keys.LAST_PERIOD_DATE] = lastPeriodDate
        }

    suspend fun setUserName(name: String) = store.edit { it[Keys.USER_NAME] = name }
    suspend fun setTheme(theme: String) = store.edit { it[Keys.THEME] = theme }
}
