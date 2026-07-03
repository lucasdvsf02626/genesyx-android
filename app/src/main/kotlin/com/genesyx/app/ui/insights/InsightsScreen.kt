package com.genesyx.app.ui.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.genesyx.app.ui.components.Eyebrow
import com.genesyx.app.ui.components.ScreenHeader
import com.genesyx.app.ui.components.tintOnWhite
import com.genesyx.app.ui.navigation.Screen
import com.genesyx.app.ui.theme.ElectricBlue
import com.genesyx.app.ui.theme.ElectricLavender
import com.genesyx.app.ui.theme.PowderBlue
import kotlin.math.sin

// Mock analytics values, ported verbatim from mockData.ts.
private val insightBars = listOf(82, 78, 90, 85, 88, 80, 92)
private val nutritionBars = listOf(60, 75, 70, 85, 78, 90, 82)

@Composable
fun InsightsScreen(
    navController: NavController,
    viewModel: InsightsViewModel = hiltViewModel(),
) {
    val colors = MaterialTheme.colorScheme
    val ph by viewModel.phInsights.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState()),
    ) {
        ScreenHeader(
            title = "Your Insights",
            subtitle = "Understanding your patterns helps you make informed, empowered decisions for your wellbeing.",
            large = true,
        )

        Column(Modifier.padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(8.dp))
            if (com.genesyx.app.core.FeatureFlags.PH_TRACKING) {
                PhInsightsSection(ph) {
                    navController.navigate(Screen.Track.route) {
                        popUpTo(Screen.Home.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            BarsCard(
                title = "Cycle regularity",
                trailing = "Last 7 cycles",
                values = insightBars,
                labels = List(7) { "C${it + 1}" },
                barHeight = 128.dp,
                brush = Brush.verticalGradient(listOf(ElectricLavender.copy(alpha = 0.8f), ElectricLavender.copy(alpha = 0.4f))),
                insight = "Your cycles are tracking with steady consistency — a small day-to-day variation is completely typical.",
            )

            Spacer(Modifier.height(12.dp))
            SymptomPatternsCard()

            Spacer(Modifier.height(12.dp))
            BarsCard(
                title = "Nutrition consistency",
                trailing = null,
                values = nutritionBars,
                labels = listOf("M", "T", "W", "T", "F", "S", "S"),
                barHeight = 112.dp,
                brush = Brush.verticalGradient(listOf(ElectricBlue, PowderBlue)),
                insight = "You've stayed close to your hydration goal four days this week — gentle progress.",
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun InsightsCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(20.dp), content = content)
    }
}

@Composable
private fun PhInsightsSection(ph: PhInsights, onOpenTracker: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenTracker),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Urine pH", style = MaterialTheme.typography.titleLarge, color = colors.onSurface)
                Text("Open tracker", style = MaterialTheme.typography.bodyMedium, color = ElectricLavender, fontWeight = FontWeight.Medium)
            }

            if (!ph.hasReadings) {
                Spacer(Modifier.height(12.dp))
                Text("No pH readings yet. Tap to log your first one.", style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
                return@Column
            }

            val status = ph.currentStatus!!
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Eyebrow("Current", color = colors.onSurfaceVariant)
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("%.1f".format(ph.currentValue), fontSize = 30.sp, fontWeight = FontWeight.SemiBold, color = status.color)
                        Spacer(Modifier.size(8.dp))
                        Box(
                            modifier = Modifier.clip(CircleShape).background(status.color.copy(alpha = 0.18f)).padding(horizontal = 10.dp, vertical = 3.dp),
                        ) {
                            Text(status.label.uppercase(), fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = status.color)
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                TrendBadge(ph.trend)
            }

            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AvgTile("7-day avg", ph.avg7, Modifier.weight(1f))
                AvgTile("30-day avg", ph.avg30, Modifier.weight(1f))
            }

            Spacer(Modifier.height(16.dp))
            Text(ph.insight, style = MaterialTheme.typography.bodyLarge, color = colors.onSurface.copy(alpha = 0.8f))
            if (ph.recommendation.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(ph.recommendation, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun TrendBadge(trend: Trend) {
    val colors = MaterialTheme.colorScheme
    val icon = when (trend) {
        Trend.UP -> Icons.Filled.ArrowUpward
        Trend.DOWN -> Icons.Filled.ArrowDownward
        Trend.FLAT -> Icons.AutoMirrored.Filled.ArrowForward
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = colors.onSurfaceVariant, modifier = Modifier.size(16.dp))
        Spacer(Modifier.size(4.dp))
        Text("vs previous", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
    }
}

@Composable
private fun AvgTile(label: String, value: Double?, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier.clip(RoundedCornerShape(16.dp)).background(colors.surfaceVariant.copy(alpha = 0.4f)).padding(14.dp),
    ) {
        Eyebrow(label, color = colors.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Text(value?.let { "%.2f".format(it) } ?: "—", style = MaterialTheme.typography.titleLarge, color = colors.onSurface)
    }
}

@Composable
private fun BarsCard(
    title: String,
    trailing: String?,
    values: List<Int>,
    labels: List<String>,
    barHeight: androidx.compose.ui.unit.Dp,
    brush: Brush,
    insight: String,
) {
    val colors = MaterialTheme.colorScheme
    InsightsCard {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = colors.onSurface)
            if (trailing != null) Text(trailing, style = MaterialTheme.typography.bodyMedium, color = ElectricLavender, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(18.dp))
        Row(
            modifier = Modifier.fillMaxWidth().height(barHeight),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            values.forEachIndexed { i, v ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(v / 100f)
                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                            .background(brush),
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(labels[i], fontSize = 10.sp, color = colors.onSurfaceVariant)
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(insight, style = MaterialTheme.typography.bodyMedium, color = colors.onSurface.copy(alpha = 0.8f))
    }
}

@Composable
private fun SymptomPatternsCard() {
    val colors = MaterialTheme.colorScheme
    InsightsCard {
        Text("Symptom patterns", style = MaterialTheme.typography.titleLarge, color = colors.onSurface)
        Spacer(Modifier.height(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(5) { r ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(7) { c ->
                        val i = r * 7 + c
                        val intensity = (sin(i * 1.7) + 1) / 2
                        val alpha = when {
                            intensity > 0.7 -> 0.5f
                            intensity > 0.4 -> 0.3f
                            intensity > 0.15 -> 0.15f
                            else -> 0.05f
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(26.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(ElectricLavender.tintOnWhite(alpha)),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(
            "Fatigue tends to ease in the second half of your cycle — useful to plan rest accordingly.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurface.copy(alpha = 0.8f),
        )
    }
}
