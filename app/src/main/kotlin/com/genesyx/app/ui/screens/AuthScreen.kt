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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.genesyx.app.data.SessionRepository
import com.genesyx.app.ui.components.GxGhostButton
import com.genesyx.app.ui.components.isValidEmail
import com.genesyx.app.ui.theme.ElectricLavender
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
) : ViewModel() {
    fun signIn(email: String, name: String?) = sessionRepository.signIn(email, name)
}

@Composable
fun AuthScreen(
    onSignedIn: () -> Unit,
    onBack: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val colors = MaterialTheme.colorScheme
    var signupMode by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    fun submit() {
        when {
            !isValidEmail(email) -> error = "Enter a valid email"
            password.length < 8 -> error = "Password must be at least 8 characters"
            password.length > 72 -> error = "Password is too long"
            else -> {
                viewModel.signIn(email.trim(), name.takeIf { signupMode })
                onSignedIn()
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
                Text("GENESYX", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = colors.onBackground, letterSpacing = 2.sp)
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
                    if (signupMode) "Save your cycle, nutrition, and partner info securely." else "Sign in to sync your journey across devices.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(28.dp))
                if (signupMode) {
                    Field("Name", name, { if (it.length <= 80) name = it; error = null }, placeholder = "Your name")
                    Spacer(Modifier.height(16.dp))
                }
                Field("Email", email, { email = it; error = null }, keyboardType = KeyboardType.Email)
                Spacer(Modifier.height(16.dp))
                Field("Password", password, { password = it; error = null }, keyboardType = KeyboardType.Password, isPassword = true)

                if (error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(error!!, style = MaterialTheme.typography.bodyMedium, color = colors.error, modifier = Modifier.fillMaxWidth())
                }

                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = { submit() },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricLavender),
                ) {
                    Text(if (signupMode) "Create account" else "Sign in", fontWeight = FontWeight.SemiBold)
                }

                Spacer(Modifier.height(24.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HorizontalDivider(Modifier.weight(1f))
                    Text("  OR  ", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                    HorizontalDivider(Modifier.weight(1f))
                }
                Spacer(Modifier.height(24.dp))

                OutlinedButton(
                    onClick = { viewModel.signIn(email.trim().ifBlank { "you@genesyx.app" }, name.ifBlank { null }); onSignedIn() },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                ) { Text("Continue with Google", color = colors.onSurface) }

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
                        modifier = Modifier.clickable { signupMode = !signupMode; error = null },
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
