package com.genesyx.app.ui.track.detail

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.genesyx.app.domain.ph.PhCopy
import com.genesyx.app.domain.ph.PhStatus
import com.genesyx.app.ui.components.CitationList
import com.genesyx.app.ui.components.ExpandableInfo
import com.genesyx.app.ui.components.HelpLinks
import com.genesyx.app.ui.components.ScreenHelpLink
import com.genesyx.app.ui.insights.PhInsightLogic
import com.genesyx.app.ui.ph.PhTrackerSection
import com.genesyx.app.ui.ph.PhTrackerViewModel

/**
 * The canonical vaginal-pH tracker, and the screen the product leans on: below the tracker card it
 * answers the four questions a reading raises — why pH matters, what this reading means, what to do
 * next, and how the Genesyx plan relates — then names its sources.
 *
 * The two band-dependent sections read the same [PhInsightLogic] the Insights screen uses, so a
 * reading is classified identically wherever it appears. Legacy urine readings are excluded there,
 * which is why a user with only pre-migration readings correctly sees the "no readings yet" copy.
 */
@Composable
fun PhDetailScreen(
    onOpenPlan: () -> Unit,
    onOpenArticle: (String) -> Unit,
    viewModel: PhTrackerViewModel = hiltViewModel(),
) {
    val readings by viewModel.readings.collectAsState()
    val status: PhStatus? = remember(readings) { PhInsightLogic.compute(readings).currentStatus }

    // A bottom tab since 12 Aug 2026: no back arrow — the bar is the way out.
    TrackerDetailScaffold(title = "Vaginal pH", onBack = null) {
        Spacer(Modifier.height(8.dp))
        PhTrackerSection(viewModel = viewModel, showHistory = true)

        Spacer(Modifier.height(16.dp))
        InfoSection(PhCopy.WHY_TITLE, PhCopy.WHY_BODY)

        Spacer(Modifier.height(12.dp))
        InfoSection(PhCopy.FERTILITY_TITLE, PhCopy.FERTILITY_BODY)

        Spacer(Modifier.height(12.dp))
        InfoSection(PhCopy.MEANS_TITLE, PhCopy.meansFor(status))

        Spacer(Modifier.height(12.dp))
        InfoSection(PhCopy.DO_TITLE, PhCopy.doNextFor(status))

        Spacer(Modifier.height(12.dp))
        TrackerDetailCard {
            Text(
                PhCopy.SUPPLEMENTS_TITLE,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                PhCopy.SUPPLEMENTS_BODY,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(
                onClick = onOpenPlan,
                contentPadding = ButtonDefaults.TextButtonContentPadding,
                modifier = Modifier.padding(top = 4.dp),
            ) { Text(PhCopy.SUPPLEMENTS_CTA) }
        }

        Spacer(Modifier.height(20.dp))
        CitationList(
            title = PhCopy.SOURCES_TITLE,
            citations = PhCopy.SOURCES,
            modifier = Modifier.padding(horizontal = 4.dp),
        )

        Spacer(Modifier.height(4.dp))
        ScreenHelpLink(
            text = HelpLinks.PH_TEXT,
            onClick = { onOpenArticle(HelpLinks.PH_SLUG) },
            modifier = Modifier.padding(horizontal = 4.dp),
        )

        Spacer(Modifier.height(12.dp))
        // The disclaimer stays one tap away rather than owning the page — the body string is
        // PhCopy.DISCLAIMER verbatim, so its pinned copy tests are untouched.
        ExpandableInfo(label = "About this tracker", body = PhCopy.DISCLAIMER)
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun InfoSection(title: String, body: String) {
    TrackerDetailCard {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
