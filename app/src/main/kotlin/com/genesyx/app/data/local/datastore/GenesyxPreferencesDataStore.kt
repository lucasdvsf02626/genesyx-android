package com.genesyx.app.data.local.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.genesyx.app.domain.hydration.HydrationFormat
import com.genesyx.app.domain.hydration.HydrationUnit
import com.genesyx.app.domain.model.FocusMode
import com.genesyx.app.domain.model.ThemeMode
import com.genesyx.app.domain.streaks.StreakEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
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
    private val json = Json { ignoreUnknownKeys = true }
    private val mapSerializer = MapSerializer(String.serializer(), String.serializer())

    private object Keys {
        val THEME = stringPreferencesKey("theme_mode")
        val PUSH = booleanPreferencesKey("push_enabled")
        val FOCUS = stringPreferencesKey("focus_mode")
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val LEARN_INTRO_SEEN = booleanPreferencesKey("learn_intro_seen")
        val PH_VAGINAL_NOTICE_SEEN = booleanPreferencesKey("ph_vaginal_notice_seen")
        val BEST_DAILY_STREAK = intPreferencesKey("best_daily_streak")
        val CELEBRATED_MILESTONES = stringSetPreferencesKey("celebrated_milestones")
        val HYDRATION_GOAL_ML = intPreferencesKey("hydration_goal_ml")
        val HYDRATION_UNIT = stringPreferencesKey("hydration_unit")
        val HYDRATION_GLASS_ML = intPreferencesKey("hydration_glass_ml")
        val FIRST_OPEN_EPOCH_DAY = longPreferencesKey("first_open_epoch_day")
        val READ_ARTICLE_SLUGS = stringSetPreferencesKey("read_article_slugs")
        val LAST_SEEN_ARTICLE_SLUG = stringPreferencesKey("last_seen_article_slug")
        val QUIZ_ANSWERS = stringPreferencesKey("quiz_answers")
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
    val learnIntroSeen: Flow<Boolean> = dataStore.data.map { it[Keys.LEARN_INTRO_SEEN] ?: false }

    /** Whether the one-time "tracker now records vaginal pH" notice has been dismissed. */
    val phVaginalNoticeSeen: Flow<Boolean> = dataStore.data.map { it[Keys.PH_VAGINAL_NOTICE_SEEN] ?: false }

    /** All-time best daily hydration streak, and the milestone ids already celebrated. */
    val bestDailyStreak: Flow<Int> = dataStore.data.map { it[Keys.BEST_DAILY_STREAK] ?: 0 }
    val celebratedMilestones: Flow<Set<String>> = dataStore.data.map { it[Keys.CELEBRATED_MILESTONES] ?: emptySet() }

    /** Her own daily water goal. Absent until she sets one, and then it is hers, not a suggestion. */
    val hydrationGoalMl: Flow<Int> = dataStore.data.map { it[Keys.HYDRATION_GOAL_ML] ?: StreakEngine.DEFAULT_GOAL_ML }

    /** How water amounts are displayed (ml/L or cups). Display-only — storage stays in ml. */
    val hydrationUnit: Flow<HydrationUnit> = dataStore.data.map { p ->
        p[Keys.HYDRATION_UNIT]?.let { runCatching { HydrationUnit.valueOf(it) }.getOrNull() } ?: HydrationUnit.ML
    }

    /** Her glass size — what one tap of a quick-add button pours. */
    val hydrationGlassMl: Flow<Int> = dataStore.data.map { it[Keys.HYDRATION_GLASS_ML] ?: HydrationFormat.DEFAULT_GLASS_ML }

    /** The epoch-day of the user's first open — the anchor the Learn drip counts weeks from. */
    val firstOpenEpochDay: Flow<Long?> = dataStore.data.map { it[Keys.FIRST_OPEN_EPOCH_DAY] }
    val readArticleSlugs: Flow<Set<String>> = dataStore.data.map { it[Keys.READ_ARTICLE_SLUGS] ?: emptySet() }
    val lastSeenArticleSlug: Flow<String?> = dataStore.data.map { it[Keys.LAST_SEEN_ARTICLE_SLUG] }

    /** Onboarding/tracking-preference answers (question id → option id), JSON-encoded. Owner data,
     *  cleared on sign-out so it never bleeds into the next account; the server row survives. */
    val quizAnswers: Flow<Map<String, String>> = dataStore.data.map { p ->
        p[Keys.QUIZ_ANSWERS]?.let {
            runCatching { json.decodeFromString(mapSerializer, it) }.getOrNull()
        } ?: emptyMap()
    }

    val signedIn: Flow<Boolean> = dataStore.data.map { it[Keys.SIGNED_IN] ?: false }
    val userId: Flow<String?> = dataStore.data.map { it[Keys.USER_ID] }
    val email: Flow<String?> = dataStore.data.map { it[Keys.EMAIL] }
    val displayName: Flow<String?> = dataStore.data.map { it[Keys.DISPLAY_NAME] }

    suspend fun setTheme(mode: ThemeMode) = dataStore.edit { it[Keys.THEME] = mode.name }.let {}
    suspend fun setPush(enabled: Boolean) = dataStore.edit { it[Keys.PUSH] = enabled }.let {}
    suspend fun setFocus(mode: FocusMode) = dataStore.edit { it[Keys.FOCUS] = mode.name }.let {}
    suspend fun setOnboardingComplete(v: Boolean) = dataStore.edit { it[Keys.ONBOARDING_COMPLETE] = v }.let {}
    suspend fun setLearnIntroSeen(v: Boolean) = dataStore.edit { it[Keys.LEARN_INTRO_SEEN] = v }.let {}
    suspend fun setPhVaginalNoticeSeen(v: Boolean) = dataStore.edit { it[Keys.PH_VAGINAL_NOTICE_SEEN] = v }.let {}
    suspend fun setBestDailyStreak(days: Int) = dataStore.edit { it[Keys.BEST_DAILY_STREAK] = days }.let {}
    suspend fun setCelebratedMilestones(ids: Set<String>) = dataStore.edit { it[Keys.CELEBRATED_MILESTONES] = ids }.let {}
    suspend fun setHydrationGoalMl(ml: Int) = dataStore.edit { it[Keys.HYDRATION_GOAL_ML] = ml }.let {}
    suspend fun setHydrationUnit(unit: HydrationUnit) = dataStore.edit { it[Keys.HYDRATION_UNIT] = unit.name }.let {}
    suspend fun setHydrationGlassMl(ml: Int) = dataStore.edit { it[Keys.HYDRATION_GLASS_ML] = ml }.let {}

    /** Set-if-absent: an existing user's anchor is the day this build first ran, never rewritten. */
    suspend fun ensureFirstOpenRecorded(epochDay: Long) = dataStore.edit {
        if (it[Keys.FIRST_OPEN_EPOCH_DAY] == null) it[Keys.FIRST_OPEN_EPOCH_DAY] = epochDay
    }.let {}

    suspend fun addReadArticleSlug(slug: String) = dataStore.edit {
        it[Keys.READ_ARTICLE_SLUGS] = (it[Keys.READ_ARTICLE_SLUGS] ?: emptySet()) + slug
    }.let {}

    suspend fun setLastSeenArticleSlug(slug: String) = dataStore.edit { it[Keys.LAST_SEEN_ARTICLE_SLUG] = slug }.let {}

    suspend fun setQuizAnswers(answers: Map<String, String>) = dataStore.edit {
        it[Keys.QUIZ_ANSWERS] = json.encodeToString(mapSerializer, answers)
    }.let {}

    /** Sign-out clears the local copy only — the server `quiz_answers` row is the owner's and stays. */
    suspend fun clearQuizAnswers() = dataStore.edit { it.remove(Keys.QUIZ_ANSWERS) }.let {}

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
