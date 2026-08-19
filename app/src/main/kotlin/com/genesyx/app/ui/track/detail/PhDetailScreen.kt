package com.genesyx.app.ui.track.detail

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.genesyx.app.core.AppLinks
import com.genesyx.app.domain.content.AppGuide
import com.genesyx.app.domain.content.LearnNavigation
import com.genesyx.app.domain.ph.PhCopy
import com.genesyx.app.domain.ph.PhStatus
import com.genesyx.app.ui.components.CitationList
import com.genesyx.app.ui.components.ExpandableInfo
import com.genesyx.app.ui.insights.PhInsightLogic
import com.genesyx.app.ui.learn.HowThisWorksLink
import com.genesyx.app.ui.ph.PhTrackerSection
import com.genesyx.app.ui.ph.PhTrackerViewModel
import java.time.LocalDate

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
    onOpenArticle: (String) -> Unit = {},
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
        InfoSection(PhCopy.ACCURACY_TITLE, PhCopy.ACCURACY_BODY)

        Spacer(Modifier.height(12.dp))
        InfoSection(PhCopy.FERTILITY_TITLE, PhCopy.FERTILITY_BODY)

        Spacer(Modifier.height(12.dp))
        TrackerDetailCard {
            Text(PhCopy.SUPPORT_TITLE, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(8.dp))
            Text(PhCopy.SUPPORT_BODY, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Text(PhCopy.SUPPORT_SIGNPOST, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(Modifier.height(12.dp))
        InfoSection(PhCopy.SEEK_HELP_TITLE, PhCopy.SEEK_HELP_BODY)

        Spacer(Modifier.height(12.dp))
        ShettlesAndScienceCard(onOpenArticle = onOpenArticle)

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

        Spacer(Modifier.height(12.dp))
        // The disclaimer stays one tap away rather than owning the page — the body string is
        // PhCopy.DISCLAIMER verbatim, so its pinned copy tests are untouched.
        ExpandableInfo(label = "About this tracker", body = PhCopy.DISCLAIMER)
        HowThisWorksLink(
            slug = AppGuide.PH,
            label = AppGuide.PH_LABEL,
            onOpen = onOpenArticle,
        )
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

@Composable
private fun ShettlesAndScienceCard(onOpenArticle: (String) -> Unit) {
    val context = LocalContext.current
    fun openUrl(url: String) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    }
    TrackerDetailCard {
        Text(PhCopy.SHETTLES_TITLE, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(8.dp))
        Text(PhCopy.SHETTLES_BODY, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        val shettlesSlug = LearnNavigation.publishedSlug(AppLinks.SHETTLES_ARTICLE_SLUG, LocalDate.now())
        if (shettlesSlug != null) {
            TextButton(
                onClick = { onOpenArticle(shettlesSlug) },
                contentPadding = ButtonDefaults.TextButtonContentPadding,
                modifier = Modifier.padding(top = 4.dp),
            ) { Text(PhCopy.SHETTLES_LINK) }
        }
        if (AppLinks.isConfiguredWebPage(AppLinks.SCIENCE_URL)) {
            TextButton(
                onClick = { openUrl(AppLinks.SCIENCE_URL) },
                contentPadding = ButtonDefaults.TextButtonContentPadding,
            ) { Text("Science and evidence on genesyx.co.uk") }
        }
    }
}
