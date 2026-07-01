package com.genesyx.app.data.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

interface AuthRepository {
    val currentUserId: Flow<String?>
    val currentUserEmail: Flow<String?>
    val isSignedIn: Boolean
    suspend fun signInWithEmail(email: String, password: String): Result<Unit>
    suspend fun signUpWithEmail(email: String, password: String, displayName: String): Result<Unit>
    suspend fun signOut()
}

@Singleton
class SupabaseAuthRepository @Inject constructor(
    private val supabase: SupabaseClient,
) : AuthRepository {

    override val currentUserId: Flow<String?> = supabase.auth.sessionStatus.map { status ->
        (status as? SessionStatus.Authenticated)?.session?.user?.id
    }

    override val currentUserEmail: Flow<String?> = supabase.auth.sessionStatus.map { status ->
        (status as? SessionStatus.Authenticated)?.session?.user?.email
    }

    override val isSignedIn: Boolean
        get() = supabase.auth.currentUserOrNull() != null

    override suspend fun signInWithEmail(email: String, password: String): Result<Unit> =
        runCatching {
            supabase.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
        }

    override suspend fun signUpWithEmail(
        email: String,
        password: String,
        displayName: String,
    ): Result<Unit> = runCatching {
        supabase.auth.signUpWith(Email) {
            this.email = email
            this.password = password
            data = kotlinx.serialization.json.buildJsonObject {
                put("display_name", kotlinx.serialization.json.JsonPrimitive(displayName))
            }
        }
    }

    override suspend fun signOut() = supabase.auth.signOut()
}
