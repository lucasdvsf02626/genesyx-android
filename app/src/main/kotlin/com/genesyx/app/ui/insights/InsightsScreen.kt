package com.genesyx.app.ui.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Remove
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.genesyx.app.ui.components.Eyebrow
import com.genesyx.app.ui.components.tintOnWhite
import com.genesyx.app.ui.navigation.Screen
import com.genesyx.app.ui.theme.ElectricLavender
import com.genesyx.app.ui.theme.PowderBlue
import kotlin.math.sin

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
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(20.dp))
        Text("Your Insights", style = MaterialTheme.typography.headlineMedium, color = colors.onBackground)
        Spacer(Modifier.height(4.dp))
        Text(
            "Gentle observations based on your tracking.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
        )

        Spacer(Modifier.height(20.dp))
        PhInsightsSection(ph) {
            navController.navigate(Screen.Track.route) {
                popUpTo(Screen.Home.route) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }

        Spacer(Modifier.height(12.dp))
        CycleRegularityCard()

        Spacer(Modifier.height(12.dp))
        SymptomPatternsCard()

        Spacer(Modifier.height(12.dp))
        NutritionConsistencyCard()

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun InsightsCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(20.dp), content = content)
    }
}

@Composable
private fun PhInsightsSection(ph: PhInsights, onOpenTracker: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    InsightsCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Urine pH", style = MaterialTheme.typography.titleLarge, color = colors.onSurface)
            Text(
                "Open tracker",
                style = MaterialTheme.typography.bodyMedium,
                color = ElectricLavender,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable(onClick = onOpenTracker),
            )
        }

        if (!ph.hasReadings) {
            Spacer(Modifier.height(12.dp))
            Text(
                "No pH readings yet — log your first in the tracker.",
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurfaceVariant,
            )
            return@InsightsCard
        }

        val status = ph.currentStatus!!
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "%.1f".format(ph.currentValue),
                fontSize = 34.sp,
                fontWeight = FontWeight.SemiBold,
                color = status.color,
            )
            Spacer(Modifier.size(10.dp))
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(status.color.copy(alpha = 0.18f))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            ) {
                Text(status.label.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = status.color)
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
        Text(ph.insight, style = MaterialTheme.typography.bodyLarge, color = colors.onSurface)
        if (ph.recommendation.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text(ph.recommendation, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        }
    }
}

@Composable
private fun TrendBadge(trend: Trend) {
    val colors = MaterialTheme.colorScheme
    val (icon, label) = when (trend) {
        Trend.UP -> Icons.Filled.ArrowUpward to "Rising"
        Trend.DOWN -> Icons.Filled.ArrowDownward to "Falling"
        Trend.FLAT -> Icons.Filled.Remove to "Steady"
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = colors.onSurfaceVariant, modifier = Modifier.size(16.dp))
        Spacer(Modifier.size(4.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
    }
}

@Composable
private fun AvgTile(label: String, value: Double?, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surfaceVariant.copy(alpha = 0.4f))
            .padding(14.dp),
    ) {
        Eyebrow(label, color = colors.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Text(
            value?.let { "%.1f".format(it) } ?: "—",
            style = MaterialTheme.typography.titleLarge,
            color = colors.onSurface,
        )
    }
}

// ── Mock analytics cards (synthetic data, mirroring the web's static charts) ──

@Composable
private fun CycleRegularityCard() {
    val colors = MaterialTheme.colorScheme
    val bars = listOf(0.7f, 0.85f, 0.6f, 0.9f, 0.75f, 0.82f, 0.7f)
    InsightsCard {
        Text("Cycle regularity", style = MaterialTheme.typography.titleLarge, color = colors.onSurface)
        Eyebrow("Last 7 cycles", color = colors.onSurfaceVariant)
        Spacer(Modifier.height(14.dp))
        BarRow(bars) { ElectricLavender.copy(alpha = 0.35f + it * 0.5f) }
        Spacer(Modifier.height(14.dp))
        Text(
            "Your recent cycles have stayed fairly consistent in length.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun NutritionConsistencyCard() {
    val colors = MaterialTheme.colorScheme
    val days = listOf("M", "T", "W", "T", "F", "S", "S")
    val bars = listOf(0.8f, 0.6f, 0.9f, 0.7f, 0.85f, 0.5f, 0.65f)
    InsightsCard {
        Text("Nutrition consistency", style = MaterialTheme.typography.titleLarge, color = colors.onSurface)
        Eyebrow("This week", color = colors.onSurfaceVariant)
        Spacer(Modifier.height(14.dp))
        BarRow(bars, labels = days) { PowderBlue.copy(alpha = 0.4f + it * 0.5f) }
        Spacer(Modifier.height(14.dp))
        Text(
            "You met your nutrition focus on most days this week.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun BarRow(values: List<Float>, labels: List<String>? = null, colorFor: (Float) -> Color) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        values.forEachIndexed { i, v ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((100 * v).dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colorFor(v)),
                )
                if (labels != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(labels[i], fontSize = 10.sp, color = colors.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun SymptomPatternsCard() {
    val colors = MaterialTheme.colorScheme
    val rows = 5
    val cols = 7
    InsightsCard {
        Text("Symptom patterns", style = MaterialTheme.typography.titleLarge, color = colors.onSurface)
        Eyebrow("Last 7 days", color = colors.onSurfaceVariant)
        Spacer(Modifier.height(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(rows) { r ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    repeat(cols) { c ->
                        // Synthetic intensity via a sine wave (matches the web's generated heatmap).
                        val raw = (sin((r * cols + c) * 0.7) + 1) / 2
                        val alpha = when {
                            raw < 0.25 -> 0.05f
                            raw < 0.5 -> 0.15f
                            raw < 0.75 -> 0.30f
                            else -> 0.50f
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
            "Symptoms tend to cluster in the days before your period.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
        )
    }
}
