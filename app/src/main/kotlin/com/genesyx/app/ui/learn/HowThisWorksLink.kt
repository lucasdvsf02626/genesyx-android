package com.genesyx.app.ui.learn

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.genesyx.app.domain.content.LearnNavigation
import com.genesyx.app.ui.theme.ElectricLavender
import java.time.LocalDate

/**
 * Quiet in-context help at the foot of a tab. Only opens a published article — a future-dated
 * slug is hidden, not shown as a dead tap.
 */
@Composable
fun HowThisWorksLink(
    slug: String,
    label: String,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier,
    today: LocalDate = LocalDate.now(),
) {
    val live = LearnNavigation.publishedSlug(slug, today) ?: return
    Text(
        label,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold,
        color = ElectricLavender,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable { onOpen(live) }
            .padding(vertical = 12.dp),
    )
}
