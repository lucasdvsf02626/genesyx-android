package com.genesyx.app.ui.components

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.genesyx.app.R
import com.genesyx.app.ui.theme.ElectricLavender

/**
 * The first-time consent ask, shown when a sign-in's server pull confirms the account has never
 * answered anywhere (ConsentRepository.needsDecision). Distinct from HealthDataConsentDialog: that
 * one describes a state she already chose; this one asks the question. "Not now" records nothing —
 * declining to answer is not a withdrawal, but collection stays off until she allows it.
 */
@Composable
fun ConsentDecisionDialog(
    onAllow: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = colors.surface,
        title = {
            Text(
                stringResource(R.string.consent_ask_title),
                style = MaterialTheme.typography.titleLarge,
                color = colors.onSurface,
            )
        },
        text = {
            Column {
                Text(
                    stringResource(R.string.consent_ask_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.consent_ask_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onAllow(); onDismiss() }) {
                Text(
                    stringResource(R.string.consent_ask_allow),
                    color = ElectricLavender,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.consent_ask_not_now), color = colors.onSurfaceVariant)
            }
        },
    )
}
