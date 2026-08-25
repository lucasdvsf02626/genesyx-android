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

        /**
         * The part of an address before the @, made presentable. The Home greeting is the first
         * line on the screen, so a raw localpart there does not read as a placeholder — it reads
         * as her name spelled wrong: "lucas.valenca" where she expects "Lucas Valença".
         *
         * Only the first character of each word is touched, so accented and non-Latin spellings
         * come through unchanged.
         */
        fun nameFromAddress(email: String): String {
            val localpart = email.substringBefore("@")
            val words = localpart.split('.', '-', '_', '+').filter { it.isNotEmpty() }
            return if (words.isEmpty()) localpart
            else words.joinToString(" ") { it.replaceFirstChar(Char::uppercaseChar) }
        }
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

    /**
     * [currentUserId] for callers that cannot afford the cold-start guess. Between process start and
     * DataStore's first emission the StateFlow still reads its `null` seed, so a signed-in woman
     * looks like a guest — which for the consent gate means reading the guest's trail and waving
     * through data collection she withdrew on her account.
     */
    suspend fun awaitUserId(): String = store.userId.first() ?: LOCAL_USER_ID

    fun signIn(email: String, name: String?, userId: String? = null) {
        scope.launch { signInNow(email, name, userId) }
    }

    /**
     * [signIn] that the caller can sequence against. `signIn` launches, so anything the auth flow
     * runs next races it — and the loser is the display name: a refresh that adopts the real
     * `profiles.display_name` can be overwritten milliseconds later by the address-derived guess.
     */
    suspend fun signInNow(email: String, name: String?, userId: String? = null) {
        // Prefer the real auth uid (Supabase) so Room rows scope to the account and match RLS;
        // else reuse an existing local id, else mint one for the guest bucket.
        val id = userId?.takeIf { it.isNotBlank() }
            ?: this.userId.value
            ?: UUID.randomUUID().toString()
        // A name that is just the localpart is not a name she gave us — both auth services
        // manufacture one when the provider withheld it — so it goes through the same
        // presentable transform as an absent name rather than reaching Home raw.
        val localpart = email.substringBefore("@")
        val given = name?.takeIf { it.isNotBlank() && !it.equals(localpart, ignoreCase = true) }
        store.setSession(
            userId = id,
            email = email,
            displayName = given ?: nameFromAddress(email),
            // The address fallback is display-only. It is a guess that often looks like a real
            // name, and a guess that reaches `profiles.display_name` overwrites the real one
            // for every device on the account.
            nameOwed = given != null,
        )
    }

    /** A name she gave us. Owed to the server until a push confirms it — see [ProfileRepository]. */
    fun updateDisplayName(name: String) {
        if (name.isNotBlank()) scope.launch { store.setDisplayName(name, owed = true) }
    }

    /** A name the server already holds. Display only: there is nothing to send back. */
    suspend fun adoptRemoteDisplayName(name: String) = store.setDisplayName(name, owed = false)

    suspend fun isNamePushOwed(): Boolean = store.pendingNamePush.first()

    suspend fun clearNamePushOwed() = store.setPendingNamePush(false)

    fun signOut() {
        scope.launch { store.clearSession() }
    }
}
