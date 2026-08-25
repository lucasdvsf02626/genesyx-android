package com.genesyx.app.ui.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.genesyx.app.ui.theme.ElectricLavender

/**
 * Grant or withdraw consent to collect health data — cycle, daily logs, pH and tracking answers.
 *
 * The two states are not symmetrical, so the copy isn't either. Granting is a plain opt-in.
 * Withdrawing has one consequence people consistently assume wrongly, so it is stated outright:
 * it stops future collection and does **not** delete what is already stored. Account deletion is a
 * separate row and stays that way — conflating them would let a tap intended as "stop tracking me"
 * destroy her history.
 */
@Composable
fun HealthDataConsentDialog(
    active: Boolean,
    onDismiss: () -> Unit,
    onGrant: () -> Unit,
    onWithdraw: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = colors.surface,
        title = {
            Text(
                "Health data consent",
                style = MaterialTheme.typography.titleLarge,
                color = colors.onSurface,
            )
        },
        text = {
            Column {
                Text(
                    if (active) {
                        "Genesyx is collecting your cycle, daily logs, pH readings and tracking " +
                            "preferences so it can show your phases, streaks and insights."
                    } else {
                        "Genesyx has stopped collecting your cycle, daily logs, pH readings and " +
                            "tracking preferences. New entries won't be saved or synced until you " +
                            "turn collection back on."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    if (active) {
                        "If you withdraw, Genesyx stops collecting new health data straight away. " +
                            "Everything you've already recorded is kept — withdrawing is not the " +
                            "same as deleting your account."
                    } else {
                        "Everything you recorded before you withdrew is still here, untouched."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { if (active) onWithdraw() else onGrant(); onDismiss() }) {
                Text(
                    if (active) "Withdraw" else "Turn on",
                    color = if (active) colors.error else ElectricLavender,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = colors.onSurfaceVariant) }
        },
    )
}
