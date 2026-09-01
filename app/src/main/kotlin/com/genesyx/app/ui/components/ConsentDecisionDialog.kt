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
import androidx.compose.ui.window.DialogProperties
import com.genesyx.app.R
import com.genesyx.app.ui.theme.ElectricLavender

/**
 * The first-time consent ask, shown when a sign-in's server pull confirms the account has never
 * answered anywhere (ConsentRepository.needsDecision). Distinct from HealthDataConsentDialog: that
 * one describes a state she already chose; this one asks the question. "Not now" records nothing —
 * declining to answer is not a withdrawal, but collection stays off until she allows it.
 *
 * Only the two buttons close it. An outside tap or Back is NOT an answer to a consent question —
 * on code 22 a stray tap dismissed the ask and left collection silently off (smoke finding,
 * CHANGELOG 1 Sep 2026). [onAllow] deliberately does not close the dialog either: it records the
 * grant, and the caller's `needsDecision` flipping false is what removes the dialog — so a grant
 * that failed to record leaves the question on screen instead of vanishing unanswered.
 */
@Composable
fun ConsentDecisionDialog(
    onAllow: () -> Unit,
    onNotNow: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = { /* only Allow / Not now close the ask */ },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
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
            TextButton(onClick = onAllow) {
                Text(
                    stringResource(R.string.consent_ask_allow),
                    color = ElectricLavender,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onNotNow) {
                Text(stringResource(R.string.consent_ask_not_now), color = colors.onSurfaceVariant)
            }
        },
    )
}
