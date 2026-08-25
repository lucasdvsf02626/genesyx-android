package com.genesyx.app.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Why the last save didn't land, shown inside the editor that tried it rather than as a toast. */
@Composable
fun SaveErrorText(message: String?) {
    if (message == null) return
    Spacer(Modifier.height(12.dp))
    Text(
        message,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
    )
}
