package com.genesyx.app.auth

import com.genesyx.app.core.log.Logger
import com.genesyx.app.core.result.DataResult
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.parseSessionFromUrl
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.user.UserSession
import io.github.jan.supabase.exceptions.RestException
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
            DataResult.Error(t.asAuthError(), t.message)
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
            if (accountAlreadyGone(t)) {
                // A retry after a dropped response: the server committed the delete, the reply was
                // lost, and this attempt found no account to delete. That IS the deletion — report
                // success so the local wipe (previously unreachable on this path) finally runs.
                logger.w("Auth", "delete-account retry found the account already gone — treating as deleted", t)
                runCatching { client.auth.signOut() }
                runCatching { client.auth.clearSession() } // the persisted token blob must go regardless
                DataResult.Success(Unit)
            } else {
                logger.e("Auth", "delete-account failed", t)
                DataResult.Error(t.asAuthError(), t.message)
            }
        }

    /**
     * Re-verifies [currentPassword] by signing in again, then updates the password on the (re-minted)
     * session. The re-auth is the point: supabase-kt's `updateUser` only needs a live session, so
     * without it anyone holding an unlocked phone could set a new password without knowing the old
     * one. A wrong current password surfaces as its own message and never reaches the update.
     */
    override suspend fun changePassword(currentPassword: String, newPassword: String): DataResult<Unit> {
        return try {
            val email = client.auth.currentSessionOrNull()?.user?.email
                ?: throw IllegalStateException("No signed-in account.")
            try {
                client.auth.signInWith(Email) {
                    this.email = email
                    this.password = currentPassword
                }
            } catch (t: Throwable) {
                // Only an answered rejection may be blamed on the typed password; a throttled or
                // unreachable re-auth keeps its own kind (the old copy mislabelled both).
                logger.e("Auth", "change-password re-auth failed", t)
                return DataResult.Error(t.asReAuthError(), t.message)
            }
            client.auth.updateUser { password = newPassword }
            DataResult.Success(Unit)
        } catch (t: Throwable) {
            logger.e("Auth", "change-password failed", t)
            DataResult.Error(t.asAuthError(), t.message)
        }
    }

    /**
     * Sends the recovery email, pointing the link back into the app ([RecoveryLink.REDIRECT_URL],
     * same URL as iOS — one shared allow-list entry). MainActivity's `genesyx://reset-password`
     * intent-filter receives it and [importRecoverySession] finishes the job.
     */
    override suspend fun resetPassword(email: String): DataResult<Unit> =
        try {
            client.auth.resetPasswordForEmail(email, redirectUrl = RecoveryLink.REDIRECT_URL)
            DataResult.Success(Unit)
        } catch (t: Throwable) {
            logger.e("Auth", "password-reset failed", t)
            DataResult.Error(t.asAuthError(), t.message)
        }

    /**
     * supabase-kt's session-from-URL handling, taken apart so failures are catchable: the library's
     * own `handleDeeplinks` throws synchronously on an error fragment (expired/used link) and
     * swallows a failed user fetch inside its own scope. Parse → fetch the user → import, and any
     * failure comes back as an Error the screen can show honestly.
     */
    override suspend fun importRecoverySession(url: String): DataResult<AuthSession> =
        try {
            val partial = client.auth.parseSessionFromUrl(url)
            val user = client.auth.retrieveUser(partial.accessToken)
            val session = partial.copy(user = user)
            client.auth.importSession(session)
            DataResult.Success(session.toAuthSession())
        } catch (t: Throwable) {
            logger.e("Auth", "recovery-session import failed", t)
            DataResult.Error(t.asAuthError(), t.message)
        }

    /** Password update on the recovery session — the emailed link already re-authenticated her. */
    override suspend fun setNewPassword(newPassword: String): DataResult<Unit> =
        try {
            client.auth.updateUser { password = newPassword }
            DataResult.Success(Unit)
        } catch (t: Throwable) {
            logger.e("Auth", "recovery password update failed", t)
            DataResult.Error(t.asAuthError(), t.message)
        }

    /** Same shape as [changePassword]: re-auth, then ask Supabase to start the email change. */
    override suspend fun changeEmail(currentPassword: String, newEmail: String): DataResult<Unit> {
        return try {
            val email = client.auth.currentSessionOrNull()?.user?.email
                ?: throw IllegalStateException("No signed-in account.")
            try {
                client.auth.signInWith(Email) {
                    this.email = email
                    this.password = currentPassword
                }
            } catch (t: Throwable) {
                // Same rule as changePassword: transport/429 must not read as a wrong password.
                logger.e("Auth", "change-email re-auth failed", t)
                return DataResult.Error(t.asReAuthError(), t.message)
            }
            client.auth.updateUser { this.email = newEmail }
            DataResult.Success(Unit)
        } catch (t: Throwable) {
            logger.e("Auth", "change-email failed", t)
            DataResult.Error(t.asAuthError(), t.message)
        }
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
                // An attempt that succeeded but minted no session is confirmation-gated sign-up.
                ?: throw AuthError(
                    AuthErrorKind.EMAIL_NOT_CONFIRMED,
                    IllegalStateException("No active session — email confirmation may be required."),
                )
            if (session.accessToken == previousToken) {
                throw IllegalStateException("$op did not establish a new session.")
            }
            if (expectedEmail != null && !session.user?.email.equals(expectedEmail.trim(), ignoreCase = true)) {
                throw IllegalStateException("$op did not establish a session for this account.")
            }
            DataResult.Success(session.toAuthSession())
        } catch (t: Throwable) {
            logger.e("Auth", "$op failed", t)
            DataResult.Error(t.asAuthError(), t.message)
        }

    /** Wrap once with the mapped kind; an [AuthError] passes through untouched. */
    private fun Throwable.asAuthError(): AuthError =
        this as? AuthError ?: AuthError(authErrorKindOf(this), this)

    /**
     * Re-auth failures only: an *answered rejection* of the password grant means the typed current
     * password is wrong; anything the server never judged (offline, throttled) keeps its own kind
     * so the dialog can't mislabel it "Current password is incorrect".
     */
    private fun Throwable.asReAuthError(): AuthError {
        val kind = authErrorKindOf(this)
        val rejected = kind == AuthErrorKind.INVALID_CREDENTIALS ||
            (kind == AuthErrorKind.UNKNOWN && this is RestException && statusCode in setOf(400, 401, 403, 422))
        return AuthError(if (rejected) AuthErrorKind.INVALID_CREDENTIALS else kind, this)
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

/**
 * True when a failed `delete_current_user` retry means the account no longer exists server-side:
 * the RPC raises `no authenticated user` (errcode 28000) when `auth.uid()` resolves to nothing,
 * and a 401 is the refreshed-token path (the auth user's refresh tokens died with the row).
 * Top-level so it is unit-testable without a SupabaseClient — see AccountAlreadyGoneTest.
 */
internal fun accountAlreadyGone(t: Throwable): Boolean {
    val rest = t as? RestException
    val text = listOfNotNull(t.message, rest?.error, rest?.description).joinToString(" ").lowercase()
    return "no authenticated user" in text || "28000" in text || rest?.statusCode == 401
}
