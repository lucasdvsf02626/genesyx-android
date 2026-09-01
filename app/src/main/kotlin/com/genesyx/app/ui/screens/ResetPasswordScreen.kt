package com.genesyx.app.ui.screens

import android.content.Intent
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.genesyx.app.R
import com.genesyx.app.auth.AuthRepository
import com.genesyx.app.auth.RecoveryLink
import com.genesyx.app.core.result.DataResult
import com.genesyx.app.ui.components.BrandLockup
import com.genesyx.app.ui.components.GxGhostButton
import com.genesyx.app.ui.theme.ElectricLavender
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The recovery deep link's landing screen: verify the link, then set a new password.
 * All copy comes from resources; error kinds map to strings in the composable, never `t.message`.
 */
sealed interface ResetPasswordUiState {
    /** Importing the recovery session the link carries. */
    data object Verifying : ResetPasswordUiState

    /** Link expired, already used, malformed, or the import failed — offer a new-link path. */
    data object Invalid : ResetPasswordUiState

    /** Session imported; the new-password form is live. */
    data class Ready(val saving: Boolean = false, @StringRes val error: Int? = null) : ResetPasswordUiState

    /** Password saved; she is signed in — navigate on. */
    data object Done : ResetPasswordUiState
}

@HiltViewModel
class ResetPasswordViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ResetPasswordUiState>(ResetPasswordUiState.Verifying)
    val uiState: StateFlow<ResetPasswordUiState> = _uiState.asStateFlow()

    init {
        // The nav library stashes the triggering intent on the entry when a deep link matched.
        val url = savedStateHandle.get<Intent>(NavController.KEY_DEEP_LINK_INTENT)?.data?.toString()
        if (url == null || !RecoveryLink.carriesSession(url)) {
            _uiState.value = ResetPasswordUiState.Invalid
        } else {
            viewModelScope.launch {
                _uiState.value = when (authRepository.completeRecovery(url)) {
                    is DataResult.Success -> ResetPasswordUiState.Ready()
                    else -> ResetPasswordUiState.Invalid
                }
            }
        }
    }

    fun submit(newPassword: String, confirm: String) {
        val current = _uiState.value as? ResetPasswordUiState.Ready ?: return
        if (current.saving) return
        val validation = when {
            newPassword.length < 8 -> R.string.reset_error_short
            newPassword.length > 72 -> R.string.reset_error_long
            newPassword != confirm -> R.string.reset_error_mismatch
            else -> null
        }
        if (validation != null) {
            _uiState.value = current.copy(error = validation)
            return
        }
        _uiState.value = current.copy(saving = true, error = null)
        viewModelScope.launch {
            _uiState.value = when (authRepository.setNewPassword(newPassword)) {
                is DataResult.Success -> ResetPasswordUiState.Done
                else -> ResetPasswordUiState.Ready(error = R.string.reset_error_failed)
            }
        }
    }
}

@Composable
fun ResetPasswordScreen(
    onDone: () -> Unit,
    onRequestNewLink: () -> Unit,
    viewModel: ResetPasswordViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    // Done never transitions back, so this fires exactly once.
    LaunchedEffect(uiState) {
        if (uiState is ResetPasswordUiState.Done) onDone()
    }
    ResetPasswordContent(
        uiState = uiState,
        onSubmit = viewModel::submit,
        onRequestNewLink = onRequestNewLink,
    )
}

@Composable
fun ResetPasswordContent(
    uiState: ResetPasswordUiState,
    onSubmit: (newPassword: String, confirm: String) -> Unit,
    onRequestNewLink: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    var newPassword by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.widthIn(max = 360.dp).fillMaxWidth()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                BrandLockup(height = 26.dp)
                Spacer(Modifier.height(24.dp))
                Text(
                    stringResource(R.string.reset_title),
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onBackground,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))

                when (uiState) {
                    ResetPasswordUiState.Verifying -> {
                        Text(
                            stringResource(R.string.reset_verifying),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(24.dp))
                        CircularProgressIndicator(color = ElectricLavender)
                        // Escape hatch if the import hangs on a bad connection.
                        Spacer(Modifier.height(16.dp))
                        GxGhostButton(text = stringResource(R.string.reset_request_new), onClick = onRequestNewLink)
                    }

                    ResetPasswordUiState.Invalid -> {
                        Text(
                            stringResource(R.string.reset_link_invalid),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.error,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(20.dp))
                        Button(
                            onClick = onRequestNewLink,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricLavender),
                        ) {
                            Text(stringResource(R.string.reset_request_new), fontWeight = FontWeight.SemiBold)
                        }
                    }

                    is ResetPasswordUiState.Ready -> {
                        Text(
                            stringResource(R.string.reset_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(28.dp))
                        PasswordField(
                            label = stringResource(R.string.reset_new_password),
                            value = newPassword,
                            onValueChange = { newPassword = it },
                        )
                        Spacer(Modifier.height(16.dp))
                        PasswordField(
                            label = stringResource(R.string.reset_confirm_password),
                            value = confirm,
                            onValueChange = { confirm = it },
                        )
                        if (uiState.error != null) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                stringResource(uiState.error),
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.error,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        Spacer(Modifier.height(20.dp))
                        Button(
                            onClick = { onSubmit(newPassword, confirm) },
                            enabled = !uiState.saving,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricLavender),
                        ) {
                            Text(
                                stringResource(if (uiState.saving) R.string.reset_saving else R.string.reset_submit),
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }

                    ResetPasswordUiState.Done -> Unit
                }
            }
        }
    }
}

@Composable
private fun PasswordField(label: String, value: String, onValueChange: (String) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        )
    }
}
