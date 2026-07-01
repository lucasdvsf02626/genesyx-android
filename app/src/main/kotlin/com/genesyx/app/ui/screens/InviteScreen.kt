package com.genesyx.app.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.genesyx.app.ui.components.Eyebrow
import com.genesyx.app.ui.components.GxBackButton
import com.genesyx.app.ui.components.GxGhostButton
import com.genesyx.app.ui.components.GxPrimaryButton
import com.genesyx.app.ui.theme.ElectricLavender
import kotlinx.coroutines.launch

@Composable
fun InviteScreen(
    code: String,
    onAccepted: () -> Unit,
    onBack: () -> Unit,
    viewModel: InviteViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var acceptCode by remember { mutableStateOf(if (code != "new") code else "") }
    val colors = MaterialTheme.colorScheme

    LaunchedEffect(state.accepted) { if (state.accepted) onAccepted() }
    LaunchedEffect(state.error) { state.error?.let { scope.launch { snackbar.showSnackbar(it) } } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        GxBackButton(onClick = onBack)
        Spacer(Modifier.height(12.dp))

        Text("Partner invite", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold, color = colors.onBackground)
        Spacer(Modifier.height(8.dp))
        Text("Share your invite code or enter a code from your partner.", style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)

        Spacer(Modifier.height(24.dp))

        if (state.myCode != null) {
            Eyebrow("Your invite code")
            Spacer(Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = ElectricLavender.copy(alpha = 0.10f)),
                elevation = CardDefaults.cardElevation(0.dp),
            ) {
                Text(
                    state.myCode!!,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = ElectricLavender,
                    modifier = Modifier.padding(24.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            GxGhostButton(text = "Share invite link", onClick = {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "Join me on Genesyx: genesyx://invite/${state.myCode}")
                }
                context.startActivity(Intent.createChooser(intent, "Share invite"))
            })
        } else {
            GxPrimaryButton(text = "Generate my invite code", onClick = viewModel::generateCode, enabled = !state.isLoading)
        }

        Spacer(Modifier.height(32.dp))
        Eyebrow("Accept a partner's invite")
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = acceptCode,
            onValueChange = { acceptCode = it },
            label = { Text("Enter invite code") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        )
        Spacer(Modifier.height(12.dp))
        GxPrimaryButton(
            text = "Accept invite",
            onClick = { viewModel.acceptInvite(acceptCode) },
            enabled = acceptCode.isNotBlank() && !state.isLoading,
        )

        SnackbarHost(snackbar)
        Spacer(Modifier.height(24.dp))
    }
}
