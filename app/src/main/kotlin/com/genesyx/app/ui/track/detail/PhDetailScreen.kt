package com.genesyx.app.ui.track.detail

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.genesyx.app.domain.ph.PhCopy
import com.genesyx.app.ui.ph.PhTrackerSection

/**
 * The canonical vaginal-pH tracker. Wraps the existing self-contained [PhTrackerSection] (latest
 * reading + status, history, trend, and the log dialog that validates 3.5–7.0 and timestamps each
 * reading), so all of the previously embedded Track behaviour moves here intact.
 */
@Composable
fun PhDetailScreen(onBack: () -> Unit) {
    TrackerDetailScaffold(title = "Vaginal pH", onBack = onBack) {
        Spacer(Modifier.height(8.dp))
        PhTrackerSection()
        Spacer(Modifier.height(16.dp))
        Text(
            PhCopy.DISCLAIMER,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Spacer(Modifier.height(32.dp))
    }
}
