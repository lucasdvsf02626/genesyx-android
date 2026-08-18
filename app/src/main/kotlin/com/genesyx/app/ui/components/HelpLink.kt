package com.genesyx.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.genesyx.app.ui.theme.ElectricLavender

/**
 * The "how does this screen work?" link that closes each main tab, matching iOS.
 *
 * Every tab ends with one of these, pointing at the bundled guide that explains that screen — the
 * app's own manual, reachable from the place it is about rather than only from the Learn tab. The
 * target must be an always-available article (no `publishedAt`), or the link would dead-end on the
 * drip gate for anyone who taps it before that article's release date.
 */
@Composable
fun ScreenHelpLink(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
            .clearAndSetSemantics { contentDescription = "$text. Opens the guide." },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        Icon(
            Icons.Outlined.HelpOutline,
            contentDescription = null,
            tint = ElectricLavender,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.size(10.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = ElectricLavender,
            modifier = Modifier.weight(1f, fill = false),
        )
        Spacer(Modifier.size(8.dp))
        Icon(
            Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = ElectricLavender,
            modifier = Modifier.size(16.dp),
        )
    }
}

/**
 * The article each tab's help link opens. Centralised so the slugs are checked in one place —
 * `ScreenHelpLinkTest` pins every one to an article that exists and is always available.
 */
object HelpLinks {
    const val HOME_SLUG = "getting-started-first-week"
    const val HOME_TEXT = "New here? What your first week looks like"

    const val TRACK_SLUG = "guide-how-the-log-works"
    const val TRACK_TEXT = "How the log works, and what each entry is for"

    const val PH_SLUG = "guide-understanding-vaginal-ph"
    const val PH_TEXT = "Read: Understanding your vaginal pH"

    const val NUTRITION_SLUG = "guide-nutrition-focus"
    const val NUTRITION_TEXT = "How your focus foods are chosen"

    const val INSIGHTS_SLUG = "reading-your-trends"
    const val INSIGHTS_TEXT = "Reading your trends without over-reading them"

    /** Every (text, slug) pair, for the test that proves each slug resolves and is undripped. */
    val all: List<Pair<String, String>> = listOf(
        HOME_TEXT to HOME_SLUG,
        TRACK_TEXT to TRACK_SLUG,
        PH_TEXT to PH_SLUG,
        NUTRITION_TEXT to NUTRITION_SLUG,
        INSIGHTS_TEXT to INSIGHTS_SLUG,
    )
}
