package com.genesyx.app.ui.learn

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.genesyx.app.domain.content.AppGuide
import com.genesyx.app.domain.content.LearnNavigation
import com.genesyx.app.domain.content.articleBySlug
import com.genesyx.app.ui.components.Eyebrow
import com.genesyx.app.ui.components.GxBackButton
import com.genesyx.app.ui.navigation.Screen
import com.genesyx.app.ui.theme.ElectricLavender
import java.time.LocalDate

/**
 * Table of contents for the how-to guides, grouped by tab. Only always-published slugs appear
 * — a date-gated week must not sit on this page.
 */
@Composable
fun HowToUseScreen(
    navController: NavController,
    today: LocalDate = LocalDate.now(),
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState()),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GxBackButton(onClick = { navController.popBackStack() })
            Text(
                "How to use Genesyx",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = colors.onBackground,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
        Text(
            "Every part of the app, and what it is for. Start anywhere — nothing here has to be read in order.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            AppGuide.byTab.forEach { (tab, entries) ->
                Spacer(Modifier.height(16.dp))
                Eyebrow(tab, color = ElectricLavender, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
                entries.forEach { entry ->
                    val article = articleBySlug(entry.slug)
                        ?.takeIf { it.publishedAt == null }
                        ?.takeIf { LearnNavigation.publishedSlug(it.slug, today) != null }
                        ?: return@forEach
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(colors.surface)
                            .clickable {
                                navController.navigate(Screen.ArticleDetail.create(article.slug))
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(article.title, style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
                            Spacer(Modifier.height(2.dp))
                            Text(entry.purpose, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                        }
                        Icon(Icons.Filled.ChevronRight, null, tint = colors.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
