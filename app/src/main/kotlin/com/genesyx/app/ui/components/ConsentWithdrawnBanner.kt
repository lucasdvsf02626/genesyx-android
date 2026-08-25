package com.genesyx.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Shown inside an editor whose saves are refused because health-data consent has been withdrawn.
 *
 * It says two things deliberately. First, where to turn collection back on — a disabled Save with no
 * explanation reads as a broken app. Second, that nothing already recorded was deleted: withdrawing
 * consent stops future collection, it is not an erasure request, and a banner that left that
 * ambiguous would push people into deleting their account to get an outcome they never asked for.
 */
@Composable
fun ConsentWithdrawnBanner(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.errorContainer)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Icon(
            Icons.Outlined.Info,
            contentDescription = null,
            tint = colors.onErrorContainer,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.size(8.dp))
        Column {
            Text(
                "Health data collection is off",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = colors.onErrorContainer,
            )
            Spacer(Modifier.size(2.dp))
            Text(
                "Turn it back on under Profile → Health data consent to save changes here. " +
                    "Everything you've already recorded has been kept.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onErrorContainer,
            )
        }
    }
}
