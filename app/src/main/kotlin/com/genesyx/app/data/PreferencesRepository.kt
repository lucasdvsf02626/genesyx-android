package com.genesyx.app.data

import com.genesyx.app.core.di.ApplicationScope
import com.genesyx.app.data.local.datastore.GenesyxPreferencesDataStore
import com.genesyx.app.domain.model.FocusMode
import com.genesyx.app.domain.model.ThemeMode
import com.genesyx.app.domain.streaks.StreakEngine
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
        store.themeMode.stateIn(scope, SharingStarted.Eagerly, ThemeMode.LIGHT)
    val pushEnabled: StateFlow<Boolean> =
        store.pushEnabled.stateIn(scope, SharingStarted.Eagerly, true)
    val focusMode: StateFlow<FocusMode> =
        store.focusMode.stateIn(scope, SharingStarted.Eagerly, FocusMode.PREP)
    val onboardingComplete: StateFlow<Boolean> =
        store.onboardingComplete.stateIn(scope, SharingStarted.Eagerly, false)
    val learnIntroSeen: StateFlow<Boolean> =
        store.learnIntroSeen.stateIn(scope, SharingStarted.Eagerly, false)
    val bestDailyStreak: StateFlow<Int> =
        store.bestDailyStreak.stateIn(scope, SharingStarted.Eagerly, 0)
    val celebratedMilestones: StateFlow<Set<String>> =
        store.celebratedMilestones.stateIn(scope, SharingStarted.Eagerly, emptySet())
    val hydrationGoalMl: StateFlow<Int> =
        store.hydrationGoalMl.stateIn(scope, SharingStarted.Eagerly, StreakEngine.DEFAULT_GOAL_ML)

    fun setTheme(mode: ThemeMode) { scope.launch { store.setTheme(mode) } }
    fun setPush(enabled: Boolean) { scope.launch { store.setPush(enabled) } }
    fun setFocus(mode: FocusMode) { scope.launch { store.setFocus(mode) } }
    fun setOnboardingComplete(complete: Boolean) { scope.launch { store.setOnboardingComplete(complete) } }
    fun setLearnIntroSeen(seen: Boolean) { scope.launch { store.setLearnIntroSeen(seen) } }
    fun setBestDailyStreak(days: Int) { scope.launch { store.setBestDailyStreak(days) } }
    fun setCelebratedMilestones(ids: Set<String>) { scope.launch { store.setCelebratedMilestones(ids) } }

    /**
     * Clamped here rather than at the call site, because this is the only writer: nothing outside
     * [StreakEngine.GOAL_RANGE_ML] can reach the store, so no reader has to defend against a zero
     * goal it would divide by.
     */
    fun setHydrationGoalMl(ml: Int) {
        scope.launch { store.setHydrationGoalMl(ml.coerceIn(StreakEngine.GOAL_RANGE_ML)) }
    }
}
