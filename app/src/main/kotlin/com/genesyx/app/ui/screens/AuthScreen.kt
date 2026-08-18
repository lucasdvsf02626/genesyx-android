package com.genesyx.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.NoCredentialException
import com.genesyx.app.BuildConfig
import com.genesyx.app.auth.AuthRepository
import com.genesyx.app.auth.GoogleCredentialClient
import com.genesyx.app.core.result.DataResult
import com.genesyx.app.ui.components.BrandLockup
import com.genesyx.app.ui.components.GxGhostButton
import com.genesyx.app.ui.components.isValidEmail
import com.genesyx.app.ui.theme.ElectricLavender
import com.genesyx.app.ui.theme.GenesyxTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Transient auth screen state (async in-flight + last error). */
data class AuthUiState(val loading: Boolean = false, val error: String? = null)

/** Transient state for the "forgot password" request, kept apart from sign-in's own state. */
data class ResetUiState(
    val sending: Boolean = false,
    val sent: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val googleClient: GoogleCredentialClient,
    private val logger: com.genesyx.app.core.log.Logger,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    /** Google sign-in is only usable when a Web client ID was compiled in (see BuildConfig). */
    val isGoogleConfigured: Boolean = BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank()

    /**
     * Launches the Credential Manager Google flow, then signs in to Supabase with the ID token.
     * No fake success: unconfigured → clear error; user-cancelled → silently dismissed; any
     * failure → a message that names the *likely* cause (so a config problem doesn't masquerade as
     * a network blip), with the raw exception logged for diagnosis.
     */
    fun signInWithGoogle(activityContext: Context, onSuccess: () -> Unit) {
        if (!isGoogleConfigured) {
            _uiState.value = AuthUiState(error = "Google sign-in isn't set up in this build. Use email and password instead.")
            return
        }
        _uiState.value = AuthUiState(loading = true)
        viewModelScope.launch {
            try {
                val idToken = googleClient.getIdToken(activityContext, BuildConfig.GOOGLE_WEB_CLIENT_ID)
                // Diagnostic only — the SHAPE of the token, never the token itself. A real Google ID
                // token starts "eyJ" and has exactly two dots; anything else (an access token, a
                // client id, empty) is the "malformed jwt" Supabase rejects. DEV-only (Logger.d is
                // suppressed in release). See the 12 Aug Supabase auth audit.
                logger.d(
                    "Auth",
                    "Google ID token: len=${idToken.length}, jwtShaped=" +
                        "${idToken.startsWith("eyJ") && idToken.count { it == '.' } == 2}",
                )
                when (val result = authRepository.signInWithGoogle(idToken)) {
                    is DataResult.Success -> {
                        _uiState.value = AuthUiState()
                        onSuccess()
                    }
                    is DataResult.Error -> {
                        // We got a Google token but the server refused it — provider disabled in
                        // Supabase, or the token's audience doesn't match. Not a device problem.
                        logger.e("Auth", "Google → Supabase rejected the token", result.throwable)
                        _uiState.value = AuthUiState(
                            error = "Google signed you in, but the server didn't accept it. " +
                                "Google sign-in may not be enabled for this app yet — email and password still works.",
                        )
                    }
                    DataResult.Loading -> Unit
                }
            } catch (e: GetCredentialCancellationException) {
                _uiState.value = AuthUiState() // user dismissed the sheet — not an error
            } catch (e: GetCredentialException) {
                logger.e("Auth", "Google credential request failed: ${e.type} — ${e.message}", e)
                _uiState.value = AuthUiState(error = googleCredentialErrorMessage(e))
            }
        }
    }

    /**
     * Turns a Credential Manager failure into a message that points at the real cause. Credential
     * Manager collapses several very different problems into a couple of exception types, so we also
     * read the (developer-facing) message for the tell-tale Google Identity status codes:
     * `10`/DEVELOPER_ERROR means the app's package + signing certificate isn't registered as an
     * Android OAuth client — the classic "works on release, fails on this build" cause.
     */
    private fun googleCredentialErrorMessage(e: GetCredentialException): String =
        googleErrorText(
            isProviderConfig = e is GetCredentialProviderConfigurationException,
            isNoCredential = e is NoCredentialException,
            rawMessage = e.message,
        )

    companion object {
        /**
         * Pure mapping from a Credential Manager failure to a user message, extracted so the
         * (message-sniffing) logic is unit-testable without constructing exceptions. Credential
         * Manager collapses very different problems into a couple of types, so the developer-facing
         * message is read for Google Identity status codes: `10`/DEVELOPER_ERROR means the app's
         * package + signing certificate isn't registered as an Android OAuth client.
         */
        internal fun googleErrorText(
            isProviderConfig: Boolean,
            isNoCredential: Boolean,
            rawMessage: String?,
        ): String {
            val detail = (rawMessage ?: "").lowercase()
            val looksLikeDevConfig = isProviderConfig ||
                "10:" in detail || "developer" in detail || "whitelist" in detail ||
                "audience" in detail || "not been allowed" in detail || "sha" in detail
            val looksLikeNetwork = "network" in detail || "7:" in detail ||
                "unable to resolve host" in detail || "timeout" in detail || "timed out" in detail
            return when {
                looksLikeDevConfig ->
                    "This build isn't registered for Google sign-in (its signing certificate isn't " +
                        "in the Google config). Email and password works — Google sign-in works on " +
                        "the Play build."
                isNoCredential ->
                    "No Google account is available on this device. Add one in your phone's " +
                        "settings, then try again."
                looksLikeNetwork ->
                    "Couldn't reach Google — check your connection and try again."
                else ->
                    "Google sign-in couldn't complete. Please try again, or use email and password."
            }
        }
    }

    /** Real email/password auth via [AuthRepository]; local-first when Supabase isn't configured. */
    fun submit(signup: Boolean, email: String, password: String, name: String?, onSuccess: () -> Unit) {
        _uiState.value = AuthUiState(loading = true)
        viewModelScope.launch {
            val result = if (signup) authRepository.signUp(email, password, name)
            else authRepository.signInWithPassword(email, password)
            when (result) {
                is DataResult.Success -> {
                    _uiState.value = AuthUiState()
                    onSuccess()
                }
                is DataResult.Error ->
                    _uiState.value = AuthUiState(error = result.message ?: "Something went wrong. Please try again.")
                DataResult.Loading -> Unit
            }
        }
    }

    fun clearError() {
        if (_uiState.value.error != null) _uiState.value = _uiState.value.copy(error = null)
    }

    private val _resetState = MutableStateFlow(ResetUiState())
    val resetState: StateFlow<ResetUiState> = _resetState.asStateFlow()

    /**
     * Ask for a password-reset email. The confirmation the UI shows is deliberately the same
     * whether or not the address has an account — see [AuthRepository.sendPasswordReset].
     */
    fun sendPasswordReset(email: String) {
        _resetState.value = ResetUiState(sending = true)
        viewModelScope.launch {
            _resetState.value = when (val result = authRepository.sendPasswordReset(email.trim())) {
                is DataResult.Success -> ResetUiState(sent = true)
                is DataResult.Error ->
                    ResetUiState(error = result.message ?: "Couldn't send the reset email. Please try again.")
                DataResult.Loading -> ResetUiState(sending = true)
            }
        }
    }

    fun clearResetState() {
        _resetState.value = ResetUiState()
    }
}

@Composable
fun AuthScreen(
    onSignedIn: () -> Unit,
    onBack: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val resetState by viewModel.resetState.collectAsState()
    val context = LocalContext.current
    AuthContent(
        uiState = uiState,
        onSubmit = { signup, email, password, name -> viewModel.submit(signup, email, password, name, onSignedIn) },
        onGoogleSignIn = { viewModel.signInWithGoogle(context, onSignedIn) },
        onClearError = viewModel::clearError,
        onBack = onBack,
        resetState = resetState,
        onSendReset = viewModel::sendPasswordReset,
        onClearReset = viewModel::clearResetState,
    )
}

@Composable
fun AuthContent(
    uiState: AuthUiState,
    onSubmit: (signup: Boolean, email: String, password: String, name: String?) -> Unit,
    onClearError: () -> Unit,
    onBack: () -> Unit,
    onGoogleSignIn: () -> Unit = {},
    resetState: ResetUiState = ResetUiState(),
    onSendReset: (String) -> Unit = {},
    onClearReset: () -> Unit = {},
) {
    val colors = MaterialTheme.colorScheme
    var signupMode by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }
    var resetOpen by remember { mutableStateOf(false) }

    val shownError = localError ?: uiState.error

    fun clearErrors() {
        localError = null
        onClearError()
    }

    fun submit() {
        when {
            !isValidEmail(email) -> localError = "Enter a valid email"
            password.length < 8 -> localError = "Password must be at least 8 characters"
            password.length > 72 -> localError = "Password is too long"
            else -> {
                localError = null
                onSubmit(signupMode, email.trim(), password, name.takeIf { signupMode })
            }
        }
    }

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
                    if (signupMode) "Create your account" else "Welcome back",
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onBackground,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    if (signupMode) "Save your cycle and nutrition info securely." else "Sign in to sync your journey across devices.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(28.dp))
                if (signupMode) {
                    Field("Name", name, { if (it.length <= 80) name = it; clearErrors() }, placeholder = "Your name")
                    Spacer(Modifier.height(16.dp))
                }
                Field("Email", email, { email = it; clearErrors() }, keyboardType = KeyboardType.Email)
                Spacer(Modifier.height(16.dp))
                Field("Password", password, { password = it; clearErrors() }, keyboardType = KeyboardType.Password, isPassword = true)

                // Sign-in only: on the sign-up form there is no account to recover yet. This is the
                // one screen a locked-out person can reach, so recovery has to live here — Profile
                // is behind the very password they have lost.
                if (!signupMode) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { onClearReset(); resetOpen = true }) {
                            Text(
                                "Forgot password?",
                                style = MaterialTheme.typography.bodyMedium,
                                color = ElectricLavender,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }

                if (shownError != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(shownError, style = MaterialTheme.typography.bodyMedium, color = colors.error, modifier = Modifier.fillMaxWidth())
                }

                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = { submit() },
                    enabled = !uiState.loading,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricLavender),
                ) {
                    Text(
                        when {
                            uiState.loading -> "Please wait…"
                            signupMode -> "Create account"
                            else -> "Sign in"
                        },
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                // "or" divider + Google sign-in via Credential Manager.
                Spacer(Modifier.height(20.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider(Modifier.weight(1f))
                    Text(
                        "  or  ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                    )
                    HorizontalDivider(Modifier.weight(1f))
                }
                Spacer(Modifier.height(16.dp))
                OutlinedButton(
                    onClick = onGoogleSignIn,
                    enabled = !uiState.loading,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Continue with Google", fontWeight = FontWeight.SemiBold)
                }

                Spacer(Modifier.height(28.dp))
                Row {
                    Text(
                        if (signupMode) "Already have an account? " else "New here? ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                    )
                    Text(
                        if (signupMode) "Sign in" else "Create account",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ElectricLavender,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { signupMode = !signupMode; clearErrors() },
                    )
                }
                Spacer(Modifier.height(4.dp))
                GxGhostButton(text = "Back to app", onClick = onBack)
            }
        }
    }

    if (resetOpen) {
        ForgotPasswordDialog(
            initialEmail = email,
            state = resetState,
            onSubmit = onSendReset,
            onDismiss = { resetOpen = false; onClearReset() },
        )
    }
}

/**
 * Sends a recovery link to a typed address.
 *
 * The confirmation never says whether the address had an account: "if an account exists…". A
 * dialog that answered differently for a registered and an unregistered address would be a free
 * account-enumeration oracle sitting on the unauthenticated screen.
 */
@Composable
private fun ForgotPasswordDialog(
    initialEmail: String,
    state: ResetUiState,
    onSubmit: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    var email by remember { mutableStateOf(initialEmail) }
    var localErr by remember { mutableStateOf<String?>(null) }
    val err = localErr ?: state.error

    AlertDialog(
        onDismissRequest = { if (!state.sending) onDismiss() },
        shape = RoundedCornerShape(20.dp),
        containerColor = colors.surface,
        title = {
            Text(
                if (state.sent) "Check your inbox" else "Reset your password",
                style = MaterialTheme.typography.titleLarge,
                color = colors.onSurface,
            )
        },
        text = {
            if (state.sent) {
                Text(
                    "If an account exists for ${email.trim()}, we've sent it a link to set a new " +
                        "password. It can take a minute to arrive — check your spam folder too.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
            } else {
                Column {
                    Text(
                        "Enter your email and we'll send you a link to set a new password.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; localErr = null },
                        label = { Text("Email") },
                        singleLine = true,
                        enabled = !state.sending,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (err != null) {
                        Spacer(Modifier.height(6.dp))
                        Text(err, style = MaterialTheme.typography.bodyMedium, color = colors.error)
                    }
                }
            }
        },
        confirmButton = {
            if (state.sent) {
                TextButton(onClick = onDismiss) {
                    Text("Done", color = ElectricLavender, fontWeight = FontWeight.SemiBold)
                }
            } else {
                TextButton(
                    enabled = !state.sending,
                    onClick = {
                        if (!isValidEmail(email)) localErr = "Enter a valid email"
                        else { localErr = null; onSubmit(email) }
                    },
                ) {
                    Text(
                        if (state.sending) "Sending…" else "Send link",
                        color = ElectricLavender,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        },
        dismissButton = {
            if (!state.sent) {
                TextButton(onClick = onDismiss, enabled = !state.sending) {
                    Text("Cancel", color = colors.onSurfaceVariant)
                }
            }
        },
    )
}

@Composable
private fun Field(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
) {
    Column(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            placeholder = placeholder?.let { { Text(it) } },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        )
    }
}

@Preview(name = "Auth — light", showBackground = true)
@Composable
private fun AuthContentLightPreview() {
    GenesyxTheme(darkTheme = false) {
        AuthContent(uiState = AuthUiState(), onSubmit = { _, _, _, _ -> }, onClearError = {}, onBack = {})
    }
}

@Preview(name = "Auth — dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AuthContentDarkPreview() {
    GenesyxTheme(darkTheme = true) {
        AuthContent(uiState = AuthUiState(), onSubmit = { _, _, _, _ -> }, onClearError = {}, onBack = {})
    }
}
