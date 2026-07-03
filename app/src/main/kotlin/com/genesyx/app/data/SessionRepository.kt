package com.genesyx.app.data

import com.genesyx.app.core.di.ApplicationScope
import com.genesyx.app.data.local.datastore.GenesyxPreferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local session state, persisted via DataStore so sign-in survives process death. Public API is
 * unchanged (StateFlows + signIn/updateDisplayName/signOut) so all existing ViewModels keep working.
 *
 * `signIn` currently mints a stable local userId used to scope every Room row per user, and marks
 * the session active. When Supabase Auth is wired (see [com.genesyx.app.auth.AuthRepository] /
 * [com.genesyx.app.auth.AuthService]) the userId becomes the Supabase auth uid and this class stays
 * the local mirror of that remote session.
 */
@Singleton
class SessionRepository @Inject constructor(
    private val store: GenesyxPreferencesDataStore,
    @ApplicationScope private val scope: CoroutineScope,
) {
    companion object {
        /** Row-scoping id used before a real account exists, so guest data stays isolated & migratable. */
        const val LOCAL_USER_ID = "local-user"
    }

    val isSignedIn: StateFlow<Boolean> =
        store.signedIn.stateIn(scope, SharingStarted.Eagerly, false)
    val userId: StateFlow<String?> =
        store.userId.stateIn(scope, SharingStarted.Eagerly, null)
    val email: StateFlow<String?> =
        store.email.stateIn(scope, SharingStarted.Eagerly, null)
    val displayName: StateFlow<String?> =
        store.displayName.stateIn(scope, SharingStarted.Eagerly, null)

    /** The id all persisted rows are scoped to: the signed-in user, or the local guest bucket. */
    fun currentUserId(): String = userId.value ?: LOCAL_USER_ID

    /** Awaits the first persisted value from DataStore. Used at launch to pick the start destination
     *  without racing the eagerly-seeded [isSignedIn] StateFlow (which reads `false` until it loads). */
    suspend fun awaitSignedIn(): Boolean = store.signedIn.first()

    fun signIn(email: String, name: String?, userId: String? = null) {
        scope.launch {
            // Prefer the real auth uid (Supabase) so Room rows scope to the account and match RLS;
            // else reuse an existing local id, else mint one for the guest bucket.
            val id = userId?.takeIf { it.isNotBlank() }
                ?: this@SessionRepository.userId.value
                ?: UUID.randomUUID().toString()
            val display = name?.takeIf { it.isNotBlank() } ?: email.substringBefore("@")
            store.setSession(userId = id, email = email, displayName = display)
        }
    }

    fun updateDisplayName(name: String) {
        if (name.isNotBlank()) scope.launch { store.setDisplayName(name) }
    }

    fun signOut() {
        scope.launch { store.clearSession() }
    }
}
