package com.genesyx.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.genesyx.app.domain.model.PhReading
import com.genesyx.app.domain.ph.PhStatus
import com.genesyx.app.ui.theme.ElectricLavender
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private enum class PhRange(val label: String, val days: Long?) {
    WEEK("7d", 7), MONTH("30d", 30), QUARTER("90d", 90), ALL("All", null)
}

private val latestFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM · HH:mm")

/**
 * Urine pH tracker card — latest reading, range filter, and a line chart with status bands.
 * Recharts has no native equivalent, so the chart is drawn with a Compose Canvas.
 * Mirrors the web PhTrackerCard (docs/SCREEN_LAYOUTS.md Track §).
 */
@Composable
fun PhTrackerCard(
    readings: List<PhReading>,
    onLogClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    var range by remember { mutableStateOf(PhRange.MONTH) }

    val filtered = remember(readings, range) {
        val days = range.days
        if (days == null) readings
        else {
            val cutoff = LocalDateTime.now().minusDays(days)
            readings.filter { it.recordedAt.isAfter(cutoff) }
        }
    }
    val latest = readings.lastOrNull()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Eyebrow("Track your pH", color = ElectricLavender)
                    Spacer(Modifier.height(2.dp))
                    Text("Urine Tracker", style = MaterialTheme.typography.titleLarge, color = colors.onSurface)
                }
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(ElectricLavender)
                        .clickable(onClick = onLogClick)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Add, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(4.dp))
                    Text("Log pH", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.height(16.dp))

            if (latest != null) {
                LatestReadingPanel(latest)
                Spacer(Modifier.height(16.dp))
                RangeSelector(range) { range = it }
                Spacer(Modifier.height(16.dp))
                if (filtered.size >= 2) {
                    PhChart(filtered, Modifier.fillMaxWidth().height(180.dp))
                } else {
                    ChartEmpty("Not enough readings in this range")
                }
            } else {
                EmptyState(onLogClick)
            }
        }
    }
}

@Composable
private fun LatestReadingPanel(latest: PhReading) {
    val colors = MaterialTheme.colorScheme
    val status = PhStatus.classify(latest.phValue)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surfaceVariant.copy(alpha = 0.4f))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(status.color.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.WaterDrop, null, tint = status.color)
        }
        Spacer(Modifier.size(14.dp))
        Column(Modifier.weight(1f)) {
            Eyebrow("Latest reading", color = colors.onSurfaceVariant)
            Text(
                "%.1f".format(latest.phValue),
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurface,
            )
            Text(
                latest.recordedAt.format(latestFormat),
                fontSize = 11.5.sp,
                color = colors.onSurfaceVariant,
            )
        }
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(status.color.copy(alpha = 0.18f))
                .padding(horizontal = 12.dp, vertical = 4.dp),
        ) {
            Text(
                status.label.uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = status.color,
            )
        }
    }
}

@Composable
private fun RangeSelector(selected: PhRange, onSelect: (PhRange) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surfaceVariant)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        PhRange.entries.forEach { r ->
            val active = r == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (active) colors.surface else Color.Transparent)
                    .clickable { onSelect(r) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    r.label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (active) colors.onSurface else colors.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PhChart(readings: List<PhReading>, modifier: Modifier = Modifier) {
    val min = PhStatus.MIN.toFloat()
    val max = PhStatus.MAX.toFloat()
    val acidic = PhStatus.ACIDIC.color
    val optimal = PhStatus.OPTIMAL.color
    val alkaline = PhStatus.ALKALINE.color

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        fun yFor(v: Float) = h - ((v - min) / (max - min)) * h

        // Status bands (acidic <6.0, optimal 6.0–7.5, alkaline >7.5)
        drawRect(acidic.copy(alpha = 0.06f), topLeft = Offset(0f, yFor(6.0f)), size = androidx.compose.ui.geometry.Size(w, h - yFor(6.0f)))
        drawRect(optimal.copy(alpha = 0.08f), topLeft = Offset(0f, yFor(7.5f)), size = androidx.compose.ui.geometry.Size(w, yFor(6.0f) - yFor(7.5f)))
        drawRect(alkaline.copy(alpha = 0.06f), topLeft = Offset(0f, 0f), size = androidx.compose.ui.geometry.Size(w, yFor(7.5f)))

        val n = readings.size
        val points = readings.mapIndexed { i, r ->
            val x = if (n == 1) w / 2 else w * i / (n - 1)
            Offset(x, yFor(r.phValue.toFloat()))
        }
        for (i in 0 until points.size - 1) {
            drawLine(
                color = ElectricLavender,
                start = points[i],
                end = points[i + 1],
                strokeWidth = 4f,
                cap = StrokeCap.Round,
            )
        }
        points.forEach { p ->
            drawCircle(ElectricLavender, radius = 5f, center = p)
            drawCircle(Color.White, radius = 2f, center = p)
        }
    }
}

@Composable
private fun ChartEmpty(message: String) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surfaceVariant.copy(alpha = 0.3f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(message, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
    }
}

@Composable
private fun EmptyState(onLogClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("No readings yet", style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
        Spacer(Modifier.height(4.dp))
        Text(
            "Log your first pH to start your chart.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        GxPrimaryButton(text = "Log pH", onClick = onLogClick, leadingIcon = Icons.Filled.Add)
    }
}
