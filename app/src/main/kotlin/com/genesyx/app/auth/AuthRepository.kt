package com.genesyx.app.auth

import com.genesyx.app.core.di.ApplicationScope
import com.genesyx.app.core.log.Logger
import com.genesyx.app.core.result.DataResult
import com.genesyx.app.data.CycleRepository
import com.genesyx.app.data.DailyLogRepository
import com.genesyx.app.data.PhRepository
import com.genesyx.app.data.ProfileRepository
import com.genesyx.app.data.SessionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
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

    fun signOut() = session.signOut()

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
