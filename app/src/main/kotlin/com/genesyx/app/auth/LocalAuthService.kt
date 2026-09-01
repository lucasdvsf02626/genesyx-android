package com.genesyx.app.auth

import com.genesyx.app.core.result.DataResult
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local-first stand-in for Supabase Auth so the app is fully usable offline / as a guest. It does
 * NOT verify passwords — that requires the real provider.
 *
 * TODO(supabase): add a SupabaseAuthService (supabase-kt Auth: signUpWith/signInWith(Email),
 * signInWith(IDToken) for Google via Credential Manager) and bind it in BindingsModule when
 * BuildConfig carries Supabase creds (AppConfig.hasSupabase).
 */
@Singleton
class LocalAuthService @Inject constructor() : AuthService {

    override suspend fun signUp(email: String, password: String, displayName: String?): DataResult<AuthSession> =
        localSession(email, displayName)

    override suspend fun signInWithPassword(email: String, password: String): DataResult<AuthSession> =
        localSession(email, null)

    override suspend fun signInWithIdToken(googleIdToken: String): DataResult<AuthSession> =
        DataResult.Error(IllegalStateException("Google sign-in requires Supabase + Credential Manager"))

    override suspend fun signOut(): DataResult<Unit> = DataResult.Success(Unit)

    override suspend fun currentSession(): AuthSession? = null

    // No remote account exists in local mode; the repository wipes local data.
    override suspend fun deleteAccount(): DataResult<Unit> = DataResult.Success(Unit)

    // Local mode stores no passwords, so there is nothing truthful to change.
    override suspend fun changePassword(currentPassword: String, newPassword: String): DataResult<Unit> =
        DataResult.Error(IllegalStateException("Password change needs the online service"), "Password change isn't available offline")

    // Same for email — there is no remote account to point at a new address.
    override suspend fun changeEmail(currentPassword: String, newEmail: String): DataResult<Unit> =
        DataResult.Error(IllegalStateException("Email change needs the online service"), "Email change isn't available offline")

    // Local mode has no inbox to write to, so there is nothing truthful to send.
    override suspend fun resetPassword(email: String): DataResult<Unit> =
        DataResult.Error(IllegalStateException("Password reset needs the online service"), "Password reset isn't available offline")

    // A recovery link can only have come from the real provider; local mode can't honour it.
    override suspend fun importRecoverySession(url: String): DataResult<AuthSession> =
        DataResult.Error(IllegalStateException("Password recovery needs the online service"))

    override suspend fun setNewPassword(newPassword: String): DataResult<Unit> =
        DataResult.Error(IllegalStateException("Password recovery needs the online service"))

    private fun localSession(email: String, displayName: String?): DataResult<AuthSession> {
        val name = displayName?.takeIf { it.isNotBlank() } ?: email.substringBefore("@")
        return DataResult.Success(
            AuthSession(
                user = AuthUser(id = UUID.randomUUID().toString(), email = email, displayName = name),
                accessToken = "local",
            ),
        )
    }
}
