package com.genesyx.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.genesyx.app.auth.AuthRepository
import com.genesyx.app.data.PartnerRepository
import com.genesyx.app.data.PreferencesRepository
import com.genesyx.app.data.ProfileRepository
import com.genesyx.app.data.SessionRepository
import com.genesyx.app.domain.model.FocusMode
import com.genesyx.app.domain.model.Partner
import com.genesyx.app.domain.model.PartnerInvite
import com.genesyx.app.domain.model.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
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

    fun setDark(dark: Boolean) {
        // Drive the live app theme (DataStore) and sync the profile row (Room + Supabase).
        preferencesRepository.setTheme(if (dark) ThemeMode.DARK else ThemeMode.SYSTEM)
        viewModelScope.launch { profileRepository.setTheme(if (dark) "dark" else "light") }
    }

    fun setPush(enabled: Boolean) = preferencesRepository.setPush(enabled)
    fun setFocus(mode: FocusMode) = preferencesRepository.setFocus(mode)

    fun updateName(name: String) {
        sessionRepository.updateDisplayName(name)
        viewModelScope.launch { profileRepository.setDisplayName(name) }
    }
    fun signOut() = sessionRepository.signOut()

    /** Permanently delete the account (remote + local). */
    fun deleteAccount() {
        viewModelScope.launch { authRepository.deleteAccount() }
    }

    fun sendInvite(email: String) = partnerRepository.sendInvite(email)
    fun revokeInvite(id: String) = partnerRepository.revoke(id)
    fun unlinkPartner() = partnerRepository.unlink()
}
