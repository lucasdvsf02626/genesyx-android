package com.genesyx.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.genesyx.app.data.PartnerRepository
import com.genesyx.app.data.SessionRepository
import com.genesyx.app.ui.components.GxGhostButton
import com.genesyx.app.ui.components.GxPrimaryButton
import com.genesyx.app.ui.theme.ElectricLavender
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class InviteViewModel @Inject constructor(
    sessionRepository: SessionRepository,
    private val partnerRepository: PartnerRepository,
) : ViewModel() {
    val isSignedIn: StateFlow<Boolean> = sessionRepository.isSignedIn
    fun accept(code: String) = partnerRepository.accept(code)
}

@Composable
fun InviteScreen(
    code: String,
    onAccepted: () -> Unit,
    onBack: () -> Unit,
    onSignIn: () -> Unit,
    viewModel: InviteViewModel = hiltViewModel(),
) {
    val colors = MaterialTheme.colorScheme
    val signedIn by viewModel.isSignedIn.collectAsState()
    val valid = code.length >= 8

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
    ) {
        Column(Modifier.widthIn(max = 360.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("GENESYX", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = colors.onBackground, letterSpacing = 2.sp)

            if (!signedIn) {
                Spacer(Modifier.height(24.dp))
                Text("You've been invited", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold, color = colors.onBackground, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text("Sign in or create an account to accept this partner invite.", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, textAlign = TextAlign.Center)
                Spacer(Modifier.height(24.dp))
                GxPrimaryButton(text = "Sign in to continue", onClick = onSignIn)
                return@Column
            }

            Spacer(Modifier.height(28.dp))
            Icon(Icons.Filled.Favorite, null, tint = ElectricLavender, modifier = Modifier.size(40.dp))
            Spacer(Modifier.height(16.dp))
            Text("Partner invite", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold, color = colors.onBackground, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))

            if (!valid) {
                Text("This invite link doesn't look valid or has already been used.", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, textAlign = TextAlign.Center)
                Spacer(Modifier.height(24.dp))
                OutlinedButton(onClick = onBack, modifier = Modifier.height(48.dp)) { Text("Back to app", color = colors.onSurface) }
            } else {
                Text(
                    "Accept to link your account so you can share your fertility-prep journey together.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(24.dp))
                GxPrimaryButton(text = "Accept invite", onClick = { viewModel.accept(code); onAccepted() })
                Spacer(Modifier.height(4.dp))
                GxGhostButton(text = "Not now", onClick = onBack)
            }
        }
    }
}
