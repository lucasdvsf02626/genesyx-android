package com.genesyx.app.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.genesyx.app.domain.content.Citation

/**
 * Renders the sources behind a piece of health copy. Each row is tappable and opens the publisher's
 * own page in the browser, so a reader can always check a claim at its origin rather than taking the
 * app's word for it.
 *
 * `runCatching` guards the launch: a device with no browser would otherwise crash the screen, and a
 * citation failing to open must never be worse than a citation not being there.
 */
@Composable
fun CitationList(
    title: String,
    citations: List<Citation>,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    Column(modifier.fillMaxWidth()) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = colors.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        citations.forEach { citation ->
            Text(
                citation.line,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(citation.url)))
                        }
                    }
                    .padding(vertical = 6.dp)
                    .clearAndSetSemantics {
                        contentDescription = "${citation.line}. Opens in your browser."
                    },
            )
        }
    }
}
