package com.genesyx.app.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory auth/session state. Stands in for Supabase Auth (`use-auth`) until the remote layer
 * is wired — `signIn` is a mock that simply marks the session active so the signed-in vs
 * signed-out UI states across Home / Profile / Partner / Invite behave correctly.
 */
@Singleton
class SessionRepository @Inject constructor() {

    private val _isSignedIn = MutableStateFlow(false)
    val isSignedIn: StateFlow<Boolean> = _isSignedIn.asStateFlow()

    private val _email = MutableStateFlow<String?>(null)
    val email: StateFlow<String?> = _email.asStateFlow()

    private val _displayName = MutableStateFlow<String?>(null)
    val displayName: StateFlow<String?> = _displayName.asStateFlow()

    fun signIn(email: String, name: String?) {
        _email.value = email
        _displayName.value = name?.takeIf { it.isNotBlank() } ?: email.substringBefore("@")
        _isSignedIn.value = true
    }

    fun updateDisplayName(name: String) {
        if (name.isNotBlank()) _displayName.value = name
    }

    fun signOut() {
        _isSignedIn.value = false
        _email.value = null
        _displayName.value = null
    }
}
