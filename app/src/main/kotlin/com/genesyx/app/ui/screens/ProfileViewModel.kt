package com.genesyx.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.genesyx.app.data.preferences.OnboardingPreferences
import com.genesyx.app.data.repository.AuthRepository
import com.genesyx.app.data.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.functions.functions
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import javax.inject.Inject

data class ProfileUiState(
    val displayName: String = "",
    val email: String = "",
    val isDarkTheme: Boolean = true,
    val hasPartner: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
    private val prefs: OnboardingPreferences,
    private val supabase: SupabaseClient,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.currentUserId.flatMapLatest { uid ->
                if (uid == null) flowOf(null) else profileRepository.getProfile(uid)
            }.collectLatest { profile ->
                _uiState.value = _uiState.value.copy(
                    displayName = profile?.displayName ?: "",
                    hasPartner = profile?.partnerId != null,
                )
            }
        }
        viewModelScope.launch {
            authRepository.currentUserEmail.collectLatest { email ->
                _uiState.value = _uiState.value.copy(email = email ?: "")
            }
        }
        viewModelScope.launch {
            prefs.theme.collectLatest { theme ->
                _uiState.value = _uiState.value.copy(isDarkTheme = theme == "dark")
            }
        }
    }

    fun setTheme(dark: Boolean) {
        viewModelScope.launch { prefs.setTheme(if (dark) "dark" else "light") }
    }

    fun signOut() {
        viewModelScope.launch { authRepository.signOut() }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            runCatching { supabase.functions.invoke("account-delete", body = buildJsonObject { }) }
            authRepository.signOut()
        }
    }
}
