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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
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
import androidx.compose.ui.res.stringResource
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.NoCredentialException
import com.genesyx.app.BuildConfig
import androidx.annotation.StringRes
import com.genesyx.app.R
import com.genesyx.app.auth.AuthErrorKind
import com.genesyx.app.auth.AuthRepository
import com.genesyx.app.auth.authErrorKind
import com.genesyx.app.auth.messageRes
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

/**
 * Transient auth screen state (async in-flight + last error + password-reset notice).
 * [error] carries only curated ViewModel copy (the Google paths); server failures arrive as
 * [errorKind] and are resolved to resources in the composable — never `t.message`.
 */
data class AuthUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val errorKind: AuthErrorKind? = null,
    val resetNotice: String? = null,
    @StringRes val resetNoticeRes: Int? = null,
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val googleClient: GoogleCredentialClient,
    private val logger: com.genesyx.app.core.log.Logger,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    /**
     * Seconds until "Forgot password?" can be tapped again. Every attempt arms it — success or
     * failure — because each tap consumes one of the very few hourly auth emails Supabase will
     * send; the old "Please try again" copy with a hot button invited burning the rest.
     */
    private val _resetCooldown = MutableStateFlow(0)
    val resetCooldown: StateFlow<Int> = _resetCooldown.asStateFlow()
    private var cooldownJob: kotlinx.coroutines.Job? = null

    private fun startResetCooldown(seconds: Int) {
        cooldownJob?.cancel()
        cooldownJob = viewModelScope.launch {
            var remaining = seconds
            while (remaining > 0) {
                _resetCooldown.value = remaining
                kotlinx.coroutines.delay(1_000)
                remaining--
            }
            _resetCooldown.value = 0
        }
    }

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
         * Neutral whether or not the address has an account. Saying "no account with that email"
         * would turn this screen into an account-enumeration oracle for a health app, and
         * Supabase deliberately does not distinguish the two either.
         */
        const val RESET_SENT =
            "If that email has an account, we've sent a reset link. Check your inbox."
        const val RESET_FAILED = "We couldn't send the reset email. Please try again."
        const val RESET_EMAIL_REQUIRED = "Enter your email first, then tap Forgot password"

        /** Post-attempt lockout on "Forgot password?" — each tap costs a scarce auth email. */
        const val RESET_COOLDOWN_SECONDS = 60

        /** Longer lockout once the server has actually said "too many" — see sendPasswordReset. */
        const val RESET_RATE_LIMITED_COOLDOWN_SECONDS = 300

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
                    _uiState.value = AuthUiState(errorKind = result.authErrorKind())
                DataResult.Loading -> Unit
            }
        }
    }

    /**
     * Emails a reset link to [email]. She is at the signed-out gate, so the address comes from
     * what she typed — not from a session. Asking for the email is not a way past the gate.
     */
    fun sendPasswordReset(email: String) {
        if (_uiState.value.loading || _resetCooldown.value > 0) return
        _uiState.value = AuthUiState(loading = true)
        viewModelScope.launch {
            when (val result = authRepository.sendPasswordReset(email)) {
                is DataResult.Success -> {
                    _uiState.value = AuthUiState(resetNotice = RESET_SENT)
                    startResetCooldown(RESET_COOLDOWN_SECONDS)
                }
                is DataResult.Error -> {
                    logger.e("Auth", "password-reset failed", result.throwable)
                    when (result.authErrorKind()) {
                        // The throttle answered: honest copy, and a long enough lockout that
                        // retrying can actually succeed (2 auth emails/hour server-side).
                        AuthErrorKind.RATE_LIMITED -> {
                            _uiState.value = AuthUiState(resetNoticeRes = R.string.auth_error_rate_limited)
                            startResetCooldown(RESET_RATE_LIMITED_COOLDOWN_SECONDS)
                        }
                        AuthErrorKind.OFFLINE -> {
                            _uiState.value = AuthUiState(resetNoticeRes = R.string.auth_error_offline)
                            startResetCooldown(RESET_COOLDOWN_SECONDS)
                        }
                        else -> {
                            _uiState.value = AuthUiState(resetNotice = RESET_FAILED)
                            startResetCooldown(RESET_COOLDOWN_SECONDS)
                        }
                    }
                }
                DataResult.Loading -> Unit
            }
        }
    }

    fun clearError() {
        if (_uiState.value.error != null) _uiState.value = _uiState.value.copy(error = null)
    }
}

@Composable
fun AuthScreen(
    onSignedIn: () -> Unit,
    onBack: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val resetCooldown by viewModel.resetCooldown.collectAsState()
    val context = LocalContext.current
    AuthContent(
        uiState = uiState,
        onSubmit = { signup, email, password, name -> viewModel.submit(signup, email, password, name, onSignedIn) },
        onGoogleSignIn = { viewModel.signInWithGoogle(context, onSignedIn) },
        onForgotPassword = viewModel::sendPasswordReset,
        onClearError = viewModel::clearError,
        onBack = onBack,
        resetCooldownSeconds = resetCooldown,
    )
}

@Composable
fun AuthContent(
    uiState: AuthUiState,
    onSubmit: (signup: Boolean, email: String, password: String, name: String?) -> Unit,
    onClearError: () -> Unit,
    onBack: () -> Unit,
    onGoogleSignIn: () -> Unit = {},
    onForgotPassword: (email: String) -> Unit = {},
    resetCooldownSeconds: Int = 0,
) {
    val colors = MaterialTheme.colorScheme
    var signupMode by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    val shownError = localError ?: uiState.error
        ?: uiState.errorKind?.let { stringResource(it.messageRes()) }
    val shownResetNotice = uiState.resetNotice
        ?: uiState.resetNoticeRes?.let { stringResource(it) }

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

    fun requestReset() {
        if (!isValidEmail(email)) {
            localError = AuthViewModel.RESET_EMAIL_REQUIRED
        } else {
            localError = null
            onForgotPassword(email.trim())
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
                    Field("Name", name, { if (it.length <= 80) name = it; clearErrors() }, placeholder = "Your name", capitalization = KeyboardCapitalization.Words)
                    Spacer(Modifier.height(16.dp))
                }
                Field("Email", email, { email = it; clearErrors() }, keyboardType = KeyboardType.Email)
                Spacer(Modifier.height(16.dp))
                Field("Password", password, { password = it; clearErrors() }, keyboardType = KeyboardType.Password, isPassword = true)

                if (shownError != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(shownError, style = MaterialTheme.typography.bodyMedium, color = colors.error, modifier = Modifier.fillMaxWidth())
                }

                // Sign-in only: there is no password to recover while creating an account, and the
                // gate makes this her single route back in — Profile's "Change password" sits
                // behind the very session she cannot obtain.
                if (!signupMode) {
                    Spacer(Modifier.height(8.dp))
                    val coolingDown = resetCooldownSeconds > 0
                    Text(
                        if (coolingDown) {
                            stringResource(R.string.auth_reset_cooldown, resetCooldownSeconds)
                        } else {
                            "Forgot password?"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (coolingDown) colors.onSurfaceVariant else ElectricLavender,
                        textAlign = TextAlign.End,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !uiState.loading && !coolingDown) { requestReset() },
                    )
                    if (shownResetNotice != null) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            shownResetNotice,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
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
}

@Composable
private fun Field(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    // Off by default because two of the three fields are an address and a password. Her name is
    // the exception, and a keyboard that refuses to capitalise filed her as "lucianne" — which is
    // then what Home greets her by.
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
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
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, capitalization = capitalization),
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
