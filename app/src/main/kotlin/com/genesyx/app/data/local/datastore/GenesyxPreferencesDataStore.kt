package com.genesyx.app.data.local.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
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
import java.time.LocalDate
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
    private val intMapSerializer = MapSerializer(String.serializer(), Int.serializer())

    private object Keys {
        val THEME = stringPreferencesKey("theme_mode")
        val PUSH = booleanPreferencesKey("push_enabled")
        val FOCUS = stringPreferencesKey("focus_mode")
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val LEARN_INTRO_SEEN = booleanPreferencesKey("learn_intro_seen")
        val BEST_DAILY_STREAK = intPreferencesKey("best_daily_streak")
        val CELEBRATED_MILESTONES = stringSetPreferencesKey("celebrated_milestones")
        val HYDRATION_GOAL_ML = intPreferencesKey("hydration_goal_ml")
        val HYDRATION_UNIT = stringPreferencesKey("hydration_unit")
        val HYDRATION_GLASS_ML = intPreferencesKey("hydration_glass_ml")
        val FIRST_OPEN_EPOCH_DAY = longPreferencesKey("first_open_epoch_day")
        val READ_ARTICLE_SLUGS = stringSetPreferencesKey("read_article_slugs")
        val READ_ARTICLE_DATES = stringSetPreferencesKey("read_article_dates")
        val LAST_SEEN_ARTICLE_SLUG = stringPreferencesKey("last_seen_article_slug")
        val LAST_SEEN_PHASE = stringPreferencesKey("last_seen_phase")
        val LAST_SEEN_PHASE_EPOCH_DAY = longPreferencesKey("last_seen_phase_epoch_day")
        val QUIZ_ANSWERS = stringPreferencesKey("quiz_answers")
        val QUIZ_ANSWERS_OWED = booleanPreferencesKey("quiz_answers_owed")
        val CYCLE_SETTINGS_OWED = booleanPreferencesKey("cycle_settings_owed")
        val SUPPLEMENT_REMINDERS = stringPreferencesKey("supplement_reminders")
        val SIGNED_IN = booleanPreferencesKey("signed_in")
        val USER_ID = stringPreferencesKey("user_id")
        val EMAIL = stringPreferencesKey("email")
        val DISPLAY_NAME = stringPreferencesKey("display_name")
        val PENDING_NAME_PUSH = booleanPreferencesKey("pending_name_push")
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
    /** ISO dates on which she opened any article — a meaningful action for the streak. */
    val articleReadDates: Flow<Set<LocalDate>> = dataStore.data.map { prefs ->
        (prefs[Keys.READ_ARTICLE_DATES] ?: emptySet()).mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }.toSet()
    }
    val lastSeenArticleSlug: Flow<String?> = dataStore.data.map { it[Keys.LAST_SEEN_ARTICLE_SLUG] }
    val lastSeenPhase: Flow<String?> = dataStore.data.map { it[Keys.LAST_SEEN_PHASE] }
    val lastSeenPhaseEpochDay: Flow<Long?> = dataStore.data.map { it[Keys.LAST_SEEN_PHASE_EPOCH_DAY] }

    /** Onboarding/tracking-preference answers (question id → option id), JSON-encoded. Owner data,
     *  cleared on sign-out so it never bleeds into the next account; the server row survives. */
    val quizAnswers: Flow<Map<String, String>> = dataStore.data.map { p ->
        p[Keys.QUIZ_ANSWERS]?.let {
            runCatching { json.decodeFromString(mapSerializer, it) }.getOrNull()
        } ?: emptyMap()
    }

    /** Whether the stored answers are an edit the server has not confirmed taking. Persisted,
     *  because an edit made offline is still owed after a relaunch. */
    val quizAnswersOwed: Flow<Boolean> = dataStore.data.map { it[Keys.QUIZ_ANSWERS_OWED] ?: false }

    /** Same owed-write contract for the cycle-settings row: a save the server has not confirmed
     *  taking is still owed after a relaunch, and refresh must push it before pulling. */
    val cycleSettingsOwed: Flow<Boolean> = dataStore.data.map { it[Keys.CYCLE_SETTINGS_OWED] ?: false }

    /** Per-supplement daily reminder times (supplement id → minutes-of-day), device-local. Not
     *  synced — a reminder schedule is a phone setting, not shared health data. */
    val supplementReminders: Flow<Map<String, Int>> = dataStore.data.map { p ->
        p[Keys.SUPPLEMENT_REMINDERS]?.let {
            runCatching { json.decodeFromString(intMapSerializer, it) }.getOrNull()
        } ?: emptyMap()
    }

    val signedIn: Flow<Boolean> = dataStore.data.map { it[Keys.SIGNED_IN] ?: false }
    val userId: Flow<String?> = dataStore.data.map { it[Keys.USER_ID] }
    val email: Flow<String?> = dataStore.data.map { it[Keys.EMAIL] }
    val displayName: Flow<String?> = dataStore.data.map { it[Keys.DISPLAY_NAME] }

    /** Whether the stored display name is a name she gave us that the server has not confirmed
     *  holding. Persisted, because a rename made offline is still owed after a relaunch. */
    val pendingNamePush: Flow<Boolean> = dataStore.data.map { it[Keys.PENDING_NAME_PUSH] ?: false }

    suspend fun setTheme(mode: ThemeMode) = dataStore.edit { it[Keys.THEME] = mode.name }.let {}
    suspend fun setPush(enabled: Boolean) = dataStore.edit { it[Keys.PUSH] = enabled }.let {}
    suspend fun setFocus(mode: FocusMode) = dataStore.edit { it[Keys.FOCUS] = mode.name }.let {}
    suspend fun setOnboardingComplete(v: Boolean) = dataStore.edit { it[Keys.ONBOARDING_COMPLETE] = v }.let {}
    suspend fun setLearnIntroSeen(v: Boolean) = dataStore.edit { it[Keys.LEARN_INTRO_SEEN] = v }.let {}
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

    suspend fun addArticleReadDate(date: LocalDate) = dataStore.edit {
        it[Keys.READ_ARTICLE_DATES] = (it[Keys.READ_ARTICLE_DATES] ?: emptySet()) + date.toString()
    }.let {}

    suspend fun setLastSeenArticleSlug(slug: String) = dataStore.edit { it[Keys.LAST_SEEN_ARTICLE_SLUG] = slug }.let {}

    suspend fun setLastSeenPhase(phase: String, epochDay: Long) = dataStore.edit {
        it[Keys.LAST_SEEN_PHASE] = phase
        it[Keys.LAST_SEEN_PHASE_EPOCH_DAY] = epochDay
    }.let {}

    suspend fun setQuizAnswers(answers: Map<String, String>) = dataStore.edit {
        it[Keys.QUIZ_ANSWERS] = json.encodeToString(mapSerializer, answers)
    }.let {}

    suspend fun setQuizAnswersOwed(owed: Boolean) =
        dataStore.edit { it[Keys.QUIZ_ANSWERS_OWED] = owed }.let {}

    suspend fun setCycleSettingsOwed(owed: Boolean) =
        dataStore.edit { it[Keys.CYCLE_SETTINGS_OWED] = owed }.let {}

    /** Sign-out clears the local copy only — the server `quiz_answers` row is the owner's and stays. */
    suspend fun clearQuizAnswers() = dataStore.edit {
        it.remove(Keys.QUIZ_ANSWERS)
        // An owed push must not outlive the session: it would fire the previous owner's answers
        // against whoever signs in next.
        it.remove(Keys.QUIZ_ANSWERS_OWED)
    }.let {}

    private fun MutablePreferences.readReminders(): Map<String, Int> =
        this[Keys.SUPPLEMENT_REMINDERS]?.let {
            runCatching { json.decodeFromString(intMapSerializer, it) }.getOrNull()
        } ?: emptyMap()

    suspend fun setSupplementReminder(id: String, minutesOfDay: Int) = dataStore.edit {
        it[Keys.SUPPLEMENT_REMINDERS] = json.encodeToString(intMapSerializer, it.readReminders() + (id to minutesOfDay))
    }.let {}

    suspend fun removeSupplementReminder(id: String) = dataStore.edit {
        it[Keys.SUPPLEMENT_REMINDERS] = json.encodeToString(intMapSerializer, it.readReminders() - id)
    }.let {}

    suspend fun clearSupplementReminders() = dataStore.edit { it.remove(Keys.SUPPLEMENT_REMINDERS) }.let {}

    suspend fun setSession(userId: String, email: String?, displayName: String?, nameOwed: Boolean) {
        dataStore.edit {
            it[Keys.SIGNED_IN] = true
            it[Keys.USER_ID] = userId
            if (email != null) it[Keys.EMAIL] = email else it.remove(Keys.EMAIL)
            if (displayName != null) it[Keys.DISPLAY_NAME] = displayName else it.remove(Keys.DISPLAY_NAME)
            it[Keys.PENDING_NAME_PUSH] = nameOwed
            // Belt and braces with clearQuizAnswers(): its only other clear runs on a fire-and-forget
            // launch during sign-out, and if that never lands the flag survives into the next
            // session — where the drain would push account A's tracking answers into account B's row.
            it[Keys.QUIZ_ANSWERS_OWED] = false
            // Same reasoning for the cycle-settings debt.
            it[Keys.CYCLE_SETTINGS_OWED] = false
        }
    }

    suspend fun setDisplayName(name: String, owed: Boolean) = dataStore.edit {
        it[Keys.DISPLAY_NAME] = name
        it[Keys.PENDING_NAME_PUSH] = owed
    }.let {}

    suspend fun setPendingNamePush(owed: Boolean) =
        dataStore.edit { it[Keys.PENDING_NAME_PUSH] = owed }.let {}

    suspend fun clearSession() {
        dataStore.edit {
            it[Keys.SIGNED_IN] = false
            it.remove(Keys.USER_ID)
            it.remove(Keys.EMAIL)
            it.remove(Keys.DISPLAY_NAME)
            // An owed push must not outlive the session, or it fires against whoever signs in next.
            it.remove(Keys.PENDING_NAME_PUSH)
        }
    }

    /**
     * Account teardown (sign-out / deletion): every key in the file goes — the session mirror, but
     * also focus mode, cycle phase, hydration goal, article history and the notification pacing
     * counters, none of which may be inherited by the device's next user. This file is the app's
     * only DataStore ("genesyx_prefs" — NotificationSettingsRepository shares it), so one clear
     * covers both repositories. First-open and onboarding re-seed themselves on the next launch.
     */
    suspend fun clearAll() {
        dataStore.edit { it.clear() }
    }
}
