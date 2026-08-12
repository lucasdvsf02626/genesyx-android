package com.genesyx.app.auth

import com.genesyx.app.core.result.DataResult

/** Authenticated user surfaced by the auth provider (Supabase Auth once wired). */
data class AuthUser(
    val id: String,
    val email: String?,
    val displayName: String?,
    val emailVerified: Boolean = false,
)

/** An active auth session: the user plus the bearer token used for RLS-scoped calls. */
data class AuthSession(
    val user: AuthUser,
    val accessToken: String,
)

/**
 * Remote authentication seam. Mirrors Supabase Auth (ARCHITECTURE.md → Auth): email/password,
 * Google id-token, sign-out, session restore. Fallible ops return [DataResult]. Until Supabase creds
 * exist, [LocalAuthService] provides a local session so the app is usable offline / as a guest.
 */
interface AuthService {
    suspend fun signUp(email: String, password: String, displayName: String?): DataResult<AuthSession>
    suspend fun signInWithPassword(email: String, password: String): DataResult<AuthSession>
    suspend fun signInWithIdToken(googleIdToken: String): DataResult<AuthSession>
    suspend fun signOut(): DataResult<Unit>
    suspend fun currentSession(): AuthSession?

    /** Permanently delete the current account (auth user + owned rows) — Play requirement. */
    suspend fun deleteAccount(): DataResult<Unit>

    /**
     * Change the signed-in user's password. [currentPassword] is re-verified against the provider
     * first so a borrowed unlocked phone can't silently take over the account.
     */
    suspend fun changePassword(currentPassword: String, newPassword: String): DataResult<Unit>

    /**
     * Change the signed-in user's email. Same re-auth gate as [changePassword]. Supabase sends a
     * confirmation link and the address only changes once it's followed — so callers must NOT
     * update the local session on Success; success here means "confirmation email sent".
     */
    suspend fun changeEmail(currentPassword: String, newEmail: String): DataResult<Unit>
}
