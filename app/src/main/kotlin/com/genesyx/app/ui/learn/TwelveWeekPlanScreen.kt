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
import androidx.compose.foundation.layout.width
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
import com.genesyx.app.domain.content.LearnDrip
import com.genesyx.app.domain.content.LearnNavigation
import com.genesyx.app.ui.components.GxBackButton
import com.genesyx.app.ui.navigation.Screen
import com.genesyx.app.ui.theme.ElectricLavender
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/**
 * Contents list for the 12-week series. Live weeks open. Future weeks show "Arrives 23 Aug"
 * and do not navigate.
 */
@Composable
fun TwelveWeekPlanScreen(
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
                "Your 12-week plan",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = colors.onBackground,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
        val intro = if (LearnDrip.weeklySeries.none { LearnDrip.isPublished(it, today) }) {
            "First article arrives 23 Aug. Until then the list names each week but none of them open."
        } else {
            "A short, honest article each Sunday. Open a week when it arrives, or come back and pick up where you left off."
        }
        Text(
            intro,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp, top = 8.dp)) {
            LearnDrip.weeklySeries.forEachIndexed { index, article ->
                val live = LearnNavigation.publishedSlug(article.slug, today)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.surface)
                        .then(
                            if (live != null) {
                                Modifier.clickable {
                                    navController.navigate(Screen.ArticleDetail.create(live))
                                }
                            } else {
                                Modifier
                            },
                        )
                        .padding(14.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(
                        "${index + 1}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = ElectricLavender,
                        modifier = Modifier.width(28.dp),
                    )
                    Column(Modifier.weight(1f)) {
                        Text(article.title, style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            if (live != null) "Ready to read" else arrivesLabel(article.publishedAt),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                        )
                    }
                    if (live != null) {
                        Icon(Icons.Filled.ChevronRight, null, tint = colors.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

internal fun arrivesLabel(date: LocalDate?): String {
    if (date == null) return "Coming soon"
    val month = date.month.getDisplayName(TextStyle.SHORT, Locale.UK)
    return "Arrives ${date.dayOfMonth} $month"
}
