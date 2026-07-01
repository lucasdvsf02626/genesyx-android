package com.genesyx.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.genesyx.app.ui.components.GxPrimaryButton
import com.genesyx.app.ui.navigation.Screen
import com.genesyx.app.ui.theme.ElectricLavender
import com.genesyx.app.ui.theme.LightDestructive

@Composable
fun ProfileScreen(
    navController: NavController,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val colors = MaterialTheme.colorScheme
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete account?") },
            text = { Text("This cannot be undone. All your data will be permanently removed.") },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; viewModel.deleteAccount() }) {
                    Text("Delete", color = LightDestructive)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(20.dp))
        Text("Profile", style = MaterialTheme.typography.headlineMedium, color = colors.onBackground)
        Spacer(Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            elevation = CardDefaults.cardElevation(0.dp),
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(state.displayName.ifBlank { "—" }, style = MaterialTheme.typography.titleLarge, color = colors.onSurface)
                Text(state.email.ifBlank { "—" }, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            elevation = CardDefaults.cardElevation(0.dp),
        ) {
            Row(
                modifier = Modifier.padding(20.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Dark mode", style = MaterialTheme.typography.bodyLarge, color = colors.onSurface, modifier = Modifier.weight(1f))
                Switch(
                    checked = state.isDarkTheme,
                    onCheckedChange = viewModel::setTheme,
                    colors = SwitchDefaults.colors(checkedThumbColor = ElectricLavender, checkedTrackColor = ElectricLavender.copy(alpha = 0.3f)),
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            elevation = CardDefaults.cardElevation(0.dp),
        ) {
            Column(Modifier.padding(20.dp)) {
                Text("Partner", style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
                Spacer(Modifier.height(4.dp))
                Text(
                    if (state.hasPartner) "Partner linked ✓" else "No partner linked",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                GxPrimaryButton(text = "Invite partner", onClick = { navController.navigate(Screen.Invite.create("new")) })
            }
        }

        Spacer(Modifier.height(24.dp))

        GxPrimaryButton(
            text = "Sign out",
            onClick = viewModel::signOut,
        )

        Spacer(Modifier.height(12.dp))

        TextButton(onClick = { showDeleteDialog = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Delete account", color = LightDestructive, style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(Modifier.height(24.dp))
    }
}
