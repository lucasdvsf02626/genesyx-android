package com.genesyx.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.genesyx.app.ui.theme.ElectricLavender

/**
 * Shown once, the moment the user flips their first reminder on — never as a cold permission call.
 * On Android 13+ the system dialog only ever shows twice; asking without context wastes one of those
 * two shots. "Nothing leaves your device" is literally true for local scheduling — and a promise: if
 * a future version adds server push, this copy has to change.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrePermissionSheet(
    onAllow: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("🔔", fontSize = 40.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                "Reminders that fit your day",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Genesyx will send a gentle nudge at the time you pick. You choose which reminders, " +
                    "when, and how often — and you can turn them off any time.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Nothing leaves your device. Reminders are scheduled locally.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onAllow,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Allow reminders")
            }
            Spacer(Modifier.height(4.dp))
            TextButton(onClick = onDismiss) {
                Text("Not right now", color = ElectricLavender)
            }
        }
    }
}
