package com.genesyx.app.auth

import com.genesyx.app.core.DispatcherProvider
import com.genesyx.app.core.di.ApplicationScope
import com.genesyx.app.core.log.Logger
import com.genesyx.app.core.result.DataResult
import com.genesyx.app.data.CycleRepository
import com.genesyx.app.data.DailyLogRepository
import com.genesyx.app.data.PhRepository
import com.genesyx.app.data.ProfileRepository
import com.genesyx.app.data.SessionRepository
import com.genesyx.app.data.local.GenesyxDatabase
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
    private val profileRepository: ProfileRepository,
    private val cycleRepository: CycleRepository,
    private val dailyLogRepository: DailyLogRepository,
    private val phRepository: PhRepository,
    private val database: GenesyxDatabase,
    private val dispatchers: DispatcherProvider,
    @ApplicationScope private val appScope: CoroutineScope,
    private val logger: Logger,
) {
    val isSignedIn: StateFlow<Boolean> = session.isSignedIn

    /** Local sign-in used by the current UI (no password verification without Supabase). */
    fun signIn(email: String, name: String?) = session.signIn(email, name)

    /** Password sign-in through the remote provider; persists the session on success. */
    suspend fun signInWithPassword(email: String, password: String): DataResult<Unit> =
        persist(authService.signInWithPassword(email, password), "sign-in")

    /** Account creation through the remote provider; persists the session on success. */
    suspend fun signUp(email: String, password: String, name: String?): DataResult<Unit> =
        persist(authService.signUp(email, password, name), "sign-up")

    /** Google sign-in via a Google ID token; persists the session on success. */
    suspend fun signInWithGoogle(idToken: String): DataResult<Unit> =
        persist(authService.signInWithIdToken(idToken), "google-sign-in")

    fun signOut() = session.signOut()

    /** Permanently delete the account remotely (RPC → cascade) then wipe all local data. */
    suspend fun deleteAccount(): DataResult<Unit> =
        when (val result = authService.deleteAccount()) {
            is DataResult.Success -> {
                withContext(dispatchers.io) { database.clearAllTables() }
                session.signOut()
                DataResult.Success(Unit)
            }
            is DataResult.Error -> {
                logger.e("Auth", "account deletion failed", result.throwable)
                result
            }
            DataResult.Loading -> DataResult.Loading
        }

    private suspend fun persist(result: DataResult<AuthSession>, op: String): DataResult<Unit> =
        when (result) {
            is DataResult.Success -> {
                val user = result.data.user
                session.signIn(user.email ?: "", user.displayName, userId = user.id)
                // Sync in the background so sign-in returns immediately and isn't blocked (or broken)
                // by a slow or failing per-table refresh. Room drives the UI reactively as each lands.
                appScope.launch {
                    profileRepository.refresh(user.id)
                    cycleRepository.refresh(user.id)
                    dailyLogRepository.refresh(user.id)
                    phRepository.refresh(user.id)
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
