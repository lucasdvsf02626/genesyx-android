package com.genesyx.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.genesyx.app.auth.AuthRepository
import com.genesyx.app.core.result.DataResult
import com.genesyx.app.data.PartnerRepository
import com.genesyx.app.data.PreferencesRepository
import com.genesyx.app.data.ProfileRepository
import com.genesyx.app.data.SessionRepository
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
) : ViewModel() {

    val isSignedIn: StateFlow<Boolean> = sessionRepository.isSignedIn
    val displayName: StateFlow<String?> = sessionRepository.displayName
    val email: StateFlow<String?> = sessionRepository.email

    val themeMode: StateFlow<ThemeMode> = preferencesRepository.themeMode
    val pushEnabled: StateFlow<Boolean> = preferencesRepository.pushEnabled
    val focusMode: StateFlow<FocusMode> = preferencesRepository.focusMode

    val partner: StateFlow<Partner?> = partnerRepository.partner
    val invites: StateFlow<List<PartnerInvite>> = partnerRepository.invites

    private val _deleting = MutableStateFlow(false)
    val deleting: StateFlow<Boolean> = _deleting.asStateFlow()
    private val _deleteError = MutableStateFlow<String?>(null)
    val deleteError: StateFlow<String?> = _deleteError.asStateFlow()
    private val _deleted = MutableStateFlow(false)
    val deleted: StateFlow<Boolean> = _deleted.asStateFlow()

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
    fun signOut() = sessionRepository.signOut()

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

    fun sendInvite(email: String) = partnerRepository.sendInvite(email)
    fun revokeInvite(id: String) = partnerRepository.revoke(id)
    fun unlinkPartner() = partnerRepository.unlink()
}
