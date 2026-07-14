package com.genesyx.app.auth

import com.genesyx.app.core.log.Logger
import com.genesyx.app.core.result.DataResult
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.user.UserSession
import io.github.jan.supabase.postgrest.postgrest
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
        establishSession("sign-up", expectedEmail = email) {
            client.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
        }

    override suspend fun signInWithPassword(email: String, password: String): DataResult<AuthSession> =
        establishSession("sign-in", expectedEmail = email) {
            client.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
        }

    override suspend fun signInWithIdToken(googleIdToken: String): DataResult<AuthSession> =
        establishSession("google-sign-in", expectedEmail = null) {
            client.auth.signInWith(IDToken) {
                idToken = googleIdToken
                provider = Google
            }
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

    /**
     * Deletes the auth user via the `delete_current_user()` SECURITY DEFINER RPC; the FK
     * `on delete cascade` on every owned table removes the user's rows. Then clears the session.
     * TODO(supabase): the RPC must be deployed server-side (see release notes / delete_current_user SQL).
     */
    override suspend fun deleteAccount(): DataResult<Unit> =
        try {
            client.postgrest.rpc("delete_current_user")
            runCatching { client.auth.signOut() } // session is now invalid — best-effort cleanup
            DataResult.Success(Unit)
        } catch (t: Throwable) {
            logger.e("Auth", "delete-account failed", t)
            DataResult.Error(t, t.message)
        }

    /**
     * Runs an auth [attempt] and returns the session it established — never an ambient one.
     *
     * supabase-kt persists the current session, so `currentSessionOrNull()` happily returns a
     * *previous* user's still-valid session. Reading it straight after an attempt means a failed
     * sign-in, or a sign-up that establishes no session (email confirmation on), falls through to
     * whoever was signed in last: they are reported as Success and the app then scopes every Room
     * row and every RLS-backed write to that stale uid. That is one user silently seated in
     * another's account, so the checks below are all-or-nothing:
     *
     *  - a session must exist afterwards;
     *  - its access token must differ from the one held before the attempt (proving this attempt,
     *    not a leftover, minted it);
     *  - for the email flows, it must belong to the address that was actually typed.
     *
     * Google carries no [expectedEmail] — the id token is opaque here — but the token-change check
     * still rules out the stale-session masquerade.
     */
    private suspend fun establishSession(
        op: String,
        expectedEmail: String?,
        attempt: suspend () -> Unit,
    ): DataResult<AuthSession> =
        try {
            val previousToken = client.auth.currentSessionOrNull()?.accessToken
            attempt()

            val session = client.auth.currentSessionOrNull()
                ?: throw IllegalStateException("No active session — email confirmation may be required.")
            if (session.accessToken == previousToken) {
                throw IllegalStateException("$op did not establish a new session.")
            }
            if (expectedEmail != null && !session.user?.email.equals(expectedEmail.trim(), ignoreCase = true)) {
                throw IllegalStateException("$op did not establish a session for this account.")
            }
            DataResult.Success(session.toAuthSession())
        } catch (t: Throwable) {
            logger.e("Auth", "$op failed", t)
            DataResult.Error(t, t.message)
        }

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
