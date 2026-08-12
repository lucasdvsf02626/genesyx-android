package com.genesyx.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.genesyx.app.auth.AuthRepository
import com.genesyx.app.core.result.DataResult
import com.genesyx.app.data.CycleRepository
import com.genesyx.app.data.PartnerRepository
import com.genesyx.app.data.PreferencesRepository
import com.genesyx.app.data.ProfileRepository
import com.genesyx.app.data.SessionRepository
import com.genesyx.app.data.SyncStatusRepository
import com.genesyx.app.domain.hydration.HydrationUnit
import com.genesyx.app.domain.model.CycleSettings
import com.genesyx.app.domain.model.FocusMode
import com.genesyx.app.domain.model.Partner
import com.genesyx.app.domain.model.PartnerInvite
import com.genesyx.app.domain.model.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val preferencesRepository: PreferencesRepository,
    private val partnerRepository: PartnerRepository,
    private val profileRepository: ProfileRepository,
    private val authRepository: AuthRepository,
    private val syncStatusRepository: SyncStatusRepository,
    private val cycleRepository: CycleRepository,
) : ViewModel() {

    val isSignedIn: StateFlow<Boolean> = sessionRepository.isSignedIn
    val displayName: StateFlow<String?> = sessionRepository.displayName
    val email: StateFlow<String?> = sessionRepository.email

    val themeMode: StateFlow<ThemeMode> = preferencesRepository.themeMode
    val pushEnabled: StateFlow<Boolean> = preferencesRepository.pushEnabled
    val focusMode: StateFlow<FocusMode> = preferencesRepository.focusMode

    val partner: StateFlow<Partner?> = partnerRepository.partner
    val invites: StateFlow<List<PartnerInvite>> = partnerRepository.invites

    /** Unsynced local changes across all synced stores — 0 means everything reached the server. */
    val pendingSync: StateFlow<Int> = syncStatusRepository.pendingCount

    fun syncNow() = syncStatusRepository.syncNow()

    // Health Profile row → the same cycle settings the Track screen edits.
    val cycleSettings: StateFlow<CycleSettings?> = cycleRepository.settings
    fun saveCycleSettings(settings: CycleSettings) = cycleRepository.upsert(settings)

    // Tracking Preferences row → the shared hydration goal + display unit + glass size.
    val hydrationGoalMl: StateFlow<Int> = preferencesRepository.hydrationGoalMl
    val hydrationUnit: StateFlow<HydrationUnit> = preferencesRepository.hydrationUnit
    val hydrationGlassMl: StateFlow<Int> = preferencesRepository.hydrationGlassMl
    fun setHydrationGoalMl(ml: Int) = preferencesRepository.setHydrationGoalMl(ml)
    fun setHydrationUnit(unit: HydrationUnit) = preferencesRepository.setHydrationUnit(unit)
    fun setHydrationGlassMl(ml: Int) = preferencesRepository.setHydrationGlassMl(ml)

    private val _deleting = MutableStateFlow(false)
    val deleting: StateFlow<Boolean> = _deleting.asStateFlow()
    private val _deleteError = MutableStateFlow<String?>(null)
    val deleteError: StateFlow<String?> = _deleteError.asStateFlow()
    private val _deleted = MutableStateFlow(false)
    val deleted: StateFlow<Boolean> = _deleted.asStateFlow()
    private val _signedOut = MutableStateFlow(false)
    val signedOut: StateFlow<Boolean> = _signedOut.asStateFlow()

    fun setTheme(mode: ThemeMode) {
        // Drive the live app theme (DataStore) and sync the profile row (Room + Supabase).
        preferencesRepository.setTheme(mode)
        viewModelScope.launch { profileRepository.setTheme(if (mode == ThemeMode.DARK) "dark" else "light") }
    }

    fun setPush(enabled: Boolean) = preferencesRepository.setPush(enabled)
    fun setFocus(mode: FocusMode) = preferencesRepository.setFocus(mode)

    fun updateName(name: String) {
        sessionRepository.updateDisplayName(name)
        viewModelScope.launch { profileRepository.setDisplayName(name) }
    }
    /**
     * Signs out remotely + locally, then signals the screen to leave. The navigation is part of the
     * fix, not polish: staying on Profile after sign-out leaves the user inside the authenticated
     * shell writing to the shared guest bucket.
     */
    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _signedOut.value = true
        }
    }

    /** Permanently delete the account (remote + local), exposing loading/error to the UI. */
    fun deleteAccount() {
        if (_deleting.value) return
        _deleteError.value = null
        _deleting.value = true
        viewModelScope.launch {
            val result = authRepository.deleteAccount()
            _deleting.value = false
            when (result) {
                is DataResult.Success -> _deleted.value = true
                is DataResult.Error ->
                    _deleteError.value = result.message ?: "Couldn't delete your account. Please try again."
                DataResult.Loading -> Unit
            }
        }
    }

    fun clearDeleteError() { _deleteError.value = null }

    private val _pwChanging = MutableStateFlow(false)
    val pwChanging: StateFlow<Boolean> = _pwChanging.asStateFlow()
    private val _pwError = MutableStateFlow<String?>(null)
    val pwError: StateFlow<String?> = _pwError.asStateFlow()
    private val _pwChanged = MutableStateFlow(false)
    val pwChanged: StateFlow<Boolean> = _pwChanged.asStateFlow()

    /** Change the account password, exposing loading/error/success to the dialog. */
    fun changePassword(current: String, new: String) {
        if (_pwChanging.value) return
        _pwError.value = null
        _pwChanging.value = true
        viewModelScope.launch {
            val result = authRepository.changePassword(current, new)
            _pwChanging.value = false
            when (result) {
                is DataResult.Success -> _pwChanged.value = true
                is DataResult.Error ->
                    _pwError.value = result.message ?: "Couldn't change your password. Please try again."
                DataResult.Loading -> Unit
            }
        }
    }

    /** Reset the dialog state when it opens or closes. */
    fun resetPasswordState() {
        _pwError.value = null
        _pwChanged.value = false
    }

    private val _emailChanging = MutableStateFlow(false)
    val emailChanging: StateFlow<Boolean> = _emailChanging.asStateFlow()
    private val _emailError = MutableStateFlow<String?>(null)
    val emailError: StateFlow<String?> = _emailError.asStateFlow()
    private val _emailChangeRequested = MutableStateFlow(false)
    val emailChangeRequested: StateFlow<Boolean> = _emailChangeRequested.asStateFlow()

    /**
     * Start an email change. Success means "confirmation email sent", NOT "email changed" — the
     * session keeps the old address until the link in the new inbox is followed, so the UI must
     * say so rather than optimistically show the new address.
     */
    fun changeEmail(currentPassword: String, newEmail: String) {
        if (_emailChanging.value) return
        _emailError.value = null
        _emailChanging.value = true
        viewModelScope.launch {
            val result = authRepository.changeEmail(currentPassword, newEmail)
            _emailChanging.value = false
            when (result) {
                is DataResult.Success -> _emailChangeRequested.value = true
                is DataResult.Error ->
                    _emailError.value = result.message ?: "Couldn't change your email. Please try again."
                DataResult.Loading -> Unit
            }
        }
    }

    /** Reset the dialog state when it opens or closes. */
    fun resetEmailState() {
        _emailError.value = null
        _emailChangeRequested.value = false
    }

    fun sendInvite(email: String) = partnerRepository.sendInvite(email)
    fun revokeInvite(id: String) = partnerRepository.revoke(id)
    fun unlinkPartner() = partnerRepository.unlink()
}
