package com.genesyx.app.ui.track.detail

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.genesyx.app.ui.ph.PhTrackerSection

/**
 * The canonical urine-pH tracker. Wraps the existing self-contained [PhTrackerSection] (latest
 * reading + status, history, trend, and the log dialog that validates 4.5–9.0 and timestamps each
 * reading), so all of the previously embedded Track behaviour moves here intact.
 */
@Composable
fun PhDetailScreen(onBack: () -> Unit) {
    TrackerDetailScaffold(title = "Urine pH", onBack = onBack) {
        Spacer(Modifier.height(8.dp))
        PhTrackerSection()
        Spacer(Modifier.height(32.dp))
    }
}
