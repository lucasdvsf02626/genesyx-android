package com.genesyx.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.genesyx.app.data.repository.AuthRepository
import com.genesyx.app.data.repository.PartnerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InviteUiState(
    val myCode: String? = null,
    val isLoading: Boolean = false,
    val accepted: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class InviteViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val partnerRepository: PartnerRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(InviteUiState())
    val uiState: StateFlow<InviteUiState> = _uiState.asStateFlow()

    fun generateCode() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        viewModelScope.launch {
            val uid = authRepository.currentUserId.first() ?: return@launch
            val result = partnerRepository.createInvite(uid, "")
            result.fold(
                onSuccess = { _uiState.value = _uiState.value.copy(myCode = it, isLoading = false) },
                onFailure = { _uiState.value = _uiState.value.copy(error = it.message, isLoading = false) },
            )
        }
    }

    fun acceptInvite(code: String) {
        _uiState.value = _uiState.value.copy(isLoading = true)
        viewModelScope.launch {
            val result = partnerRepository.acceptInvite(code.trim())
            result.fold(
                onSuccess = { _uiState.value = _uiState.value.copy(accepted = true, isLoading = false) },
                onFailure = { _uiState.value = _uiState.value.copy(error = it.message ?: "Invalid code.", isLoading = false) },
            )
        }
    }
}
