package com.genesyx.app.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.genesyx.app.core.result.DataResult
import com.genesyx.app.data.remote.WaitlistRemoteDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Stores the waitlist signup server-side before showing success — the confirmation screen promises
 * a guide by email, so the address must actually be recorded somewhere the team can act on.
 */
@HiltViewModel
class WaitlistViewModel @Inject constructor(
    private val remote: WaitlistRemoteDataSource,
) : ViewModel() {

    private val _submitting = MutableStateFlow(false)
    val submitting: StateFlow<Boolean> = _submitting.asStateFlow()
    private val _submitted = MutableStateFlow(false)
    val submitted: StateFlow<Boolean> = _submitted.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun join(email: String) {
        if (_submitting.value || _submitted.value) return
        _error.value = null
        _submitting.value = true
        viewModelScope.launch {
            val result = remote.join(email)
            _submitting.value = false
            when (result) {
                is DataResult.Success -> _submitted.value = true
                is DataResult.Error ->
                    _error.value = "Couldn't join right now — check your connection and try again."
                DataResult.Loading -> Unit
            }
        }
    }

    fun clearError() { _error.value = null }
}
