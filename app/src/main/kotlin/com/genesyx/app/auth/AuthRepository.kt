package com.genesyx.app.auth

import com.genesyx.app.core.DispatcherProvider
import com.genesyx.app.core.di.ApplicationScope
import com.genesyx.app.core.log.Logger
import com.genesyx.app.core.result.DataResult
import com.genesyx.app.data.ConsentRepository
import com.genesyx.app.data.CycleRepository
import com.genesyx.app.data.DailyLogRepository
import com.genesyx.app.data.PhRepository
import com.genesyx.app.data.ProfileRepository
import com.genesyx.app.data.QuizAnswersRepository
import com.genesyx.app.data.SessionRepository
import com.genesyx.app.data.SupplementReminderRepository
import com.genesyx.app.data.SyncStatusRepository
import com.genesyx.app.data.UserSupplementRepository
import com.genesyx.app.data.local.GenesyxDatabase
import com.genesyx.app.data.local.datastore.GenesyxPreferencesDataStore
import com.genesyx.app.notifications.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Coordinates authentication: calls the remote [AuthService] and persists the resulting session
 * locally via [SessionRepository] (DataStore-backed). Today the bound AuthService is local-first, so
 * sign-in mirrors the existing behaviour — but it is now persisted and routed through the real seam,
 * ready to switch to Supabase without touching the UI.
 */
@Singleton
class AuthRepository @Inject constructor(
    private val authService: AuthService,
    private val session: SessionRepository,
    private val consentRepository: ConsentRepository,
    private val profileRepository: ProfileRepository,
    private val cycleRepository: CycleRepository,
    private val dailyLogRepository: DailyLogRepository,
    private val phRepository: PhRepository,
    private val userSupplementRepository: UserSupplementRepository,
    private val quizAnswersRepository: QuizAnswersRepository,
    private val supplementReminderRepository: SupplementReminderRepository,
    private val syncStatusRepository: SyncStatusRepository,
    private val preferences: GenesyxPreferencesDataStore,
    private val database: GenesyxDatabase,
    private val reminderScheduler: ReminderScheduler,
    private val dispatchers: DispatcherProvider,
    @ApplicationScope private val appScope: CoroutineScope,
    private val logger: Logger,
) {
    val isSignedIn: StateFlow<Boolean> = session.isSignedIn

    /** Password sign-in through the remote provider; persists the session on success. */
    suspend fun signInWithPassword(email: String, password: String): DataResult<Unit> =
        persist(authService.signInWithPassword(email, password), "sign-in")

    /**
     * Account creation through the remote provider; persists the session on success.
     *
     * [name] is carried past the provider rather than read back off the returned session:
     * SupabaseAuthService builds its `AuthUser` from the address alone, so the name she typed on
     * the sign-up form was being dropped somewhere between this call and the greeting on Home.
     */
    suspend fun signUp(email: String, password: String, name: String?): DataResult<Unit> =
        persist(authService.signUp(email, password, name), "sign-up", typedName = name)

    /** Google sign-in via a Google ID token; persists the session on success. */
    suspend fun signInWithGoogle(idToken: String): DataResult<Unit> =
        persist(authService.signInWithIdToken(idToken), "google-sign-in")

    /**
     * Ends the session everywhere: the remote provider, the local Room cache, then the persisted
     * session itself.
     *
     * Clearing Room is not housekeeping. Without it the previous user's cycle, logs and pH rows sit
     * on disk after they log out, and — because [SessionRepository.currentUserId] falls back to the
     * shared [SessionRepository.LOCAL_USER_ID] bucket once the session is gone — whatever the *next*
     * person on the device logs while signed out lands in that same bucket and is read straight
     * back. Signed-in data is already write-through to Supabase and is re-pulled by `persist` on the
     * next sign-in, so there is nothing to lose by dropping the local copy.
     *
     * The remote sign-out is best-effort: if it fails (offline), the local session must still go, or
     * "Log out" would leave the user signed in. But supabase-kt's own session must be cleared, or a
     * later sign-up would inherit it — see [AuthService.signOut].
     */
    suspend fun signOut(): DataResult<Unit> {
        val remote = authService.signOut()
        if (remote is DataResult.Error) {
            logger.w("Auth", "remote sign-out failed; clearing local session anyway")
        }
        wipeLocalState()
        return DataResult.Success(Unit)
    }

    /** Change the signed-in user's password via the remote provider. */
    suspend fun changePassword(currentPassword: String, newPassword: String): DataResult<Unit> {
        val result = authService.changePassword(currentPassword, newPassword)
        if (result is DataResult.Error) logger.e("Auth", "change-password failed", result.throwable)
        return result
    }

    /**
     * Email a password-reset link to the address she typed. The signed-out path cannot read an
     * address off the session — that is the whole point of this method. Success means "the
     * provider accepted the request"; it must never persist a session.
     */
    suspend fun sendPasswordReset(email: String): DataResult<Unit> {
        val result = authService.resetPassword(email)
        if (result is DataResult.Error) logger.e("Auth", "password-reset failed", result.throwable)
        return result
    }

    /**
     * Finish a password-recovery deep link: import the session it carries and persist it exactly
     * like a sign-in (the link is the proof of ownership). On success she is signed in and the
     * usual post-sign-in adoption + refreshes run.
     */
    suspend fun completeRecovery(url: String): DataResult<Unit> =
        persist(authService.importRecoverySession(url), "password-recovery")

    /** Set a new password on the recovery session ([completeRecovery] must have succeeded). */
    suspend fun setNewPassword(newPassword: String): DataResult<Unit> {
        val result = authService.setNewPassword(newPassword)
        if (result is DataResult.Error) logger.e("Auth", "recovery password update failed", result.throwable)
        return result
    }

    /**
     * Start an email change via the remote provider. Success means "confirmation email sent" — the
     * persisted session deliberately keeps the old address until Supabase confirms the new one.
     */
    suspend fun changeEmail(currentPassword: String, newEmail: String): DataResult<Unit> {
        val result = authService.changeEmail(currentPassword, newEmail)
        if (result is DataResult.Error) logger.e("Auth", "change-email failed", result.throwable)
        return result
    }

    /** Permanently delete the account remotely (RPC → cascade) then wipe all local data. */
    suspend fun deleteAccount(): DataResult<Unit> =
        when (val result = authService.deleteAccount()) {
            is DataResult.Success -> {
                wipeLocalState()
                DataResult.Success(Unit)
            }
            is DataResult.Error -> {
                logger.e("Auth", "account deletion failed", result.throwable)
                result
            }
            DataResult.Loading -> DataResult.Loading
        }

    /**
     * The one local teardown, shared by sign-out and deletion. Sequential and AWAITED — the caller
     * reports success only after every store is empty; the old fire-and-forget launches let the UI
     * navigate away while quiz answers, reminder times and the signed-in flag were still on disk.
     *
     * Order: cancel everything that could wake and rewrite (reminder chains name her supplements
     * in their inputData; sync drains would push against a dead session), then Room, then the
     * whole prefs file — session mirror, quiz answers, reminder times, focus mode, cycle phase,
     * hydration goal, article history, notification pacing. Nothing here may be inherited by the
     * device's next user, and clearing DataStore last means a crash mid-wipe still reads signed-in
     * and retries rather than stranding half-cleared state behind a signed-out flag.
     */
    private suspend fun wipeLocalState() {
        reminderScheduler.cancelAll()
        supplementReminderRepository.clearAllNow()
        syncStatusRepository.cancelDrains()
        withContext(dispatchers.io) { database.clearAllTables() }
        preferences.clearAll()
    }

    private suspend fun persist(
        result: DataResult<AuthSession>,
        op: String,
        typedName: String? = null,
    ): DataResult<Unit> =
        when (result) {
            is DataResult.Success -> {
                val user = result.data.user
                val name = typedName?.takeIf { it.isNotBlank() } ?: user.displayName
                // Awaited, not launched: the refreshes below adopt the server's display name, and a
                // signIn still in flight would overwrite it with the address-derived guess.
                session.signInNow(user.email ?: "", name, userId = user.id)
                // Before any of it: if she withdrew consent as a guest, that answer is keyed to the
                // guest bucket and this account's trail is empty — which reads as permitted. Carry
                // it across first, or the pulls below run under a consent she revoked.
                consentRepository.adoptGuestDecision(user.id)
                // Sync in the background so sign-in returns immediately and isn't blocked (or broken)
                // by a slow or failing per-table refresh. Room drives the UI reactively as each lands.
                appScope.launch {
                    // Consent FIRST: the pulls and drains below run under whatever answer the
                    // account holds, and a reinstall's empty local trail must not outvote a
                    // withdrawal recorded on the server (that was the defect — a wiped device
                    // silently reversed it).
                    consentRepository.refresh(user.id)
                    profileRepository.refresh(user.id)
                    // Adopt-before-pull, store by store: adopted rows/settings must exist before
                    // the refresh that would otherwise pull the server copy over nothing — or, for
                    // daily logs, before a later clearAllTables wipes the guest bucket unread.
                    cycleRepository.adoptGuestSettings(user.id)
                    cycleRepository.refresh(user.id)
                    dailyLogRepository.adoptGuestLogs(user.id)
                    dailyLogRepository.refresh(user.id)
                    // Adopt guest pH readings BEFORE the pull: adopted rows are PENDING_UPSERT, and
                    // refresh's merge never overwrites unsynced local rows, so they survive the pull
                    // and its trailing syncPending() pushes them.
                    phRepository.adoptGuestReadings(user.id)
                    phRepository.refresh(user.id)
                    // Same adopt-before-pull dance for the user's own supplements.
                    userSupplementRepository.adoptGuestEntries(user.id)
                    userSupplementRepository.refresh(user.id)
                    // Quiz answers: server wins if present, else the guest's local answers are adopted.
                    quizAnswersRepository.refresh(user.id)
                }
                DataResult.Success(Unit)
            }
            is DataResult.Error -> {
                logger.e("Auth", "$op failed", result.throwable)
                result
            }
            DataResult.Loading -> DataResult.Loading
        }
}
