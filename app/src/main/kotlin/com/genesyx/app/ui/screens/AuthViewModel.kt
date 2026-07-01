package com.genesyx.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.genesyx.app.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val displayName: String = "",
    val isSignUp: Boolean = false,
    val isLoading: Boolean = false,
    val signedIn: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun onEmailChange(v: String) { _uiState.value = _uiState.value.copy(email = v, error = null) }
    fun onPasswordChange(v: String) { _uiState.value = _uiState.value.copy(password = v, error = null) }
    fun onDisplayNameChange(v: String) { _uiState.value = _uiState.value.copy(displayName = v) }
    fun toggleMode() { _uiState.value = _uiState.value.copy(isSignUp = !_uiState.value.isSignUp, error = null) }

    fun submit() {
        val s = _uiState.value
        if (s.email.isBlank() || s.password.isBlank()) {
            _uiState.value = s.copy(error = "Please enter your email and password.")
            return
        }
        _uiState.value = s.copy(isLoading = true, error = null)
        viewModelScope.launch {
            val result = if (s.isSignUp) {
                authRepository.signUpWithEmail(s.email.trim(), s.password, s.displayName.trim())
            } else {
                authRepository.signInWithEmail(s.email.trim(), s.password)
            }
            result.fold(
                onSuccess = { _uiState.value = _uiState.value.copy(signedIn = true, isLoading = false) },
                onFailure = { _uiState.value = _uiState.value.copy(isLoading = false, error = it.message ?: "Sign-in failed.") },
            )
        }
    }
}
