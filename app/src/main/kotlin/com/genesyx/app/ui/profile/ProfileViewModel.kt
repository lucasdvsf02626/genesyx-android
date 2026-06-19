package com.genesyx.app.ui.profile

import androidx.lifecycle.ViewModel
import com.genesyx.app.data.PartnerRepository
import com.genesyx.app.data.PreferencesRepository
import com.genesyx.app.data.SessionRepository
import com.genesyx.app.domain.model.FocusMode
import com.genesyx.app.domain.model.Partner
import com.genesyx.app.domain.model.PartnerInvite
import com.genesyx.app.domain.model.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val preferencesRepository: PreferencesRepository,
    private val partnerRepository: PartnerRepository,
) : ViewModel() {

    val isSignedIn: StateFlow<Boolean> = sessionRepository.isSignedIn
    val displayName: StateFlow<String?> = sessionRepository.displayName
    val email: StateFlow<String?> = sessionRepository.email

    val themeMode: StateFlow<ThemeMode> = preferencesRepository.themeMode
    val pushEnabled: StateFlow<Boolean> = preferencesRepository.pushEnabled
    val focusMode: StateFlow<FocusMode> = preferencesRepository.focusMode

    val partner: StateFlow<Partner?> = partnerRepository.partner
    val invites: StateFlow<List<PartnerInvite>> = partnerRepository.invites

    fun setDark(dark: Boolean) = preferencesRepository.setTheme(if (dark) ThemeMode.DARK else ThemeMode.SYSTEM)
    fun setPush(enabled: Boolean) = preferencesRepository.setPush(enabled)
    fun setFocus(mode: FocusMode) = preferencesRepository.setFocus(mode)

    fun updateName(name: String) = sessionRepository.updateDisplayName(name)
    fun signOut() = sessionRepository.signOut()

    fun sendInvite(email: String) = partnerRepository.sendInvite(email)
    fun revokeInvite(id: String) = partnerRepository.revoke(id)
    fun unlinkPartner() = partnerRepository.unlink()
}
