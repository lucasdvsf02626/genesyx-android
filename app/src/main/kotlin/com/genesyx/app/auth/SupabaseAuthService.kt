package com.genesyx.app.auth

import com.genesyx.app.core.log.Logger
import com.genesyx.app.core.result.DataResult
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.user.UserSession
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real Supabase Auth implementation (supabase-kt). Bound as [AuthService] only when Supabase creds
 * are configured (see NetworkModule.provideAuthService); otherwise [LocalAuthService] keeps the app
 * usable local-first. Mirrors the web auth contract (ARCHITECTURE.md → Auth): email/password now,
 * Google id-token ready for when the Android token flow is added.
 */
@Singleton
class SupabaseAuthService @Inject constructor(
    private val client: SupabaseClient,
    private val logger: Logger,
) : AuthService {

    override suspend fun signUp(email: String, password: String, displayName: String?): DataResult<AuthSession> =
        try {
            client.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
            DataResult.Success(requireSession())
        } catch (t: Throwable) {
            logger.e("Auth", "sign-up failed", t)
            DataResult.Error(t, t.message)
        }

    override suspend fun signInWithPassword(email: String, password: String): DataResult<AuthSession> =
        try {
            client.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            DataResult.Success(requireSession())
        } catch (t: Throwable) {
            logger.e("Auth", "sign-in failed", t)
            DataResult.Error(t, t.message)
        }

    override suspend fun signInWithIdToken(googleIdToken: String): DataResult<AuthSession> =
        try {
            client.auth.signInWith(IDToken) {
                idToken = googleIdToken
                provider = Google
            }
            DataResult.Success(requireSession())
        } catch (t: Throwable) {
            logger.e("Auth", "google sign-in failed", t)
            DataResult.Error(t, t.message)
        }

    override suspend fun signOut(): DataResult<Unit> =
        try {
            client.auth.signOut()
            DataResult.Success(Unit)
        } catch (t: Throwable) {
            logger.e("Auth", "sign-out failed", t)
            DataResult.Error(t, t.message)
        }

    override suspend fun currentSession(): AuthSession? =
        client.auth.currentSessionOrNull()?.toAuthSession()

    private fun requireSession(): AuthSession =
        client.auth.currentSessionOrNull()?.toAuthSession()
            ?: throw IllegalStateException("No active session — email confirmation may be required.")

    private fun UserSession.toAuthSession(): AuthSession {
        val u = user
        return AuthSession(
            user = AuthUser(
                id = u?.id.orEmpty(),
                email = u?.email,
                displayName = u?.email?.substringBefore("@"),
                emailVerified = u?.emailConfirmedAt != null,
            ),
            accessToken = accessToken,
        )
    }
}
