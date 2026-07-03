package com.genesyx.app.ui.clients

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.genesyx.app.domain.model.Client
import com.genesyx.app.ui.components.Eyebrow
import com.genesyx.app.ui.components.GxGhostButton
import com.genesyx.app.ui.components.GxPrimaryButton
import com.genesyx.app.ui.components.ScreenHeader
import com.genesyx.app.ui.components.isValidEmail
import com.genesyx.app.ui.theme.BabyLavender
import com.genesyx.app.ui.theme.ElectricLavender
import com.genesyx.app.ui.theme.ElectricPink

/**
 * Client management list — the usable surface of the multi-client scaling path. Backed by
 * [ClientsViewModel] → [com.genesyx.app.data.ClientRepository] (local-first, owner-scoped). Uses a
 * LazyColumn so it stays smooth well past 100 records.
 */
@Composable
fun ClientsScreen(onBack: () -> Unit, viewModel: ClientsViewModel = hiltViewModel()) {
    val colors = MaterialTheme.colorScheme
    val clients by viewModel.clients.collectAsState()
    var addOpen by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(colors.background)) {
        ScreenHeader(
            title = "Clients",
            subtitle = when (clients.size) {
                0 -> "No clients yet"
                1 -> "1 client"
                else -> "${clients.size} clients"
            },
            onBack = onBack,
        )

        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
            GxPrimaryButton(
                text = "Add client",
                onClick = { addOpen = true },
                modifier = Modifier.weight(1f),
                leadingIcon = Icons.Filled.Add,
            )
        }

        if (clients.isEmpty()) {
            EmptyState(onSeed = { viewModel.seedDemo() })
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
            ) {
                items(clients, key = { it.id }) { client ->
                    ClientCard(client = client, onDelete = { viewModel.deleteClient(client.id) })
                }
            }
        }
    }

    if (addOpen) {
        AddClientDialog(
            onDismiss = { addOpen = false },
            onAdd = { name, email -> viewModel.addClient(name, email); addOpen = false },
        )
    }
}

@Composable
private fun ClientCard(client: Client, onDelete: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape)
                .background(Brush.linearGradient(listOf(BabyLavender, ElectricPink))),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                client.displayName.firstOrNull()?.uppercase() ?: "C",
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text(client.displayName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = colors.onSurface)
            if (client.email != null) {
                Text(client.email, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete client", tint = colors.error)
        }
    }
}

@Composable
private fun EmptyState(onSeed: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier.size(64.dp).clip(CircleShape).background(ElectricLavender.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Group, null, tint = ElectricLavender, modifier = Modifier.size(30.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text("No clients yet", style = MaterialTheme.typography.titleLarge, color = colors.onBackground)
        Spacer(Modifier.height(6.dp))
        Text(
            "Add clients to manage them here. Each record is stored per account and stays private.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))
        GxGhostButton(text = "Seed 100 demo clients", onClick = onSeed)
    }
}

@Composable
private fun AddClientDialog(onDismiss: () -> Unit, onAdd: (name: String, email: String?) -> Unit) {
    val colors = MaterialTheme.colorScheme
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = colors.surface,
        title = { Text("Add client", style = MaterialTheme.typography.titleLarge, color = colors.onSurface) },
        text = {
            Column {
                Eyebrow("Name", color = colors.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 80) name = it; error = null },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                Eyebrow("Email (optional)", color = colors.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; error = null },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (error != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(error!!, style = MaterialTheme.typography.bodyMedium, color = colors.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                when {
                    name.isBlank() -> error = "Enter a name"
                    email.isNotBlank() && !isValidEmail(email) -> error = "Enter a valid email"
                    else -> onAdd(name, email.ifBlank { null })
                }
            }) { Text("Add", color = ElectricLavender, fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = colors.onSurfaceVariant) } },
    )
}
