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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.genesyx.app.domain.model.PhMeasurement
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
 * Vaginal pH tracker card — latest reading, range filter, and a line chart with status bands.
 * Legacy urine readings (pre-migration) render muted/hollow and don't join the line; the y-axis and
 * bands are the vaginal scale only. The chart is drawn with a Compose Canvas.
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
                    Text("Vaginal pH Tracker", style = MaterialTheme.typography.titleLarge, color = colors.onSurface)
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

            Spacer(Modifier.height(12.dp))
            Text(
                "pH entries sync to your Genesyx account.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LatestReadingPanel(latest: PhReading) {
    val colors = MaterialTheme.colorScheme
    // Legacy urine readings are on a different scale, so we don't classify them as Healthy/Elevated —
    // they show a neutral "urine (legacy)" marker instead.
    val isLegacy = latest.measurementType == PhMeasurement.URINE
    val accent = if (isLegacy) colors.onSurfaceVariant else PhStatus.classify(latest.phValue).color
    val pillLabel = if (isLegacy) "Urine (legacy)".uppercase() else PhStatus.classify(latest.phValue).label.uppercase()
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
                .background(accent.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.WaterDrop, null, tint = accent)
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
                .background(accent.copy(alpha = 0.18f))
                .padding(horizontal = 12.dp, vertical = 4.dp),
        ) {
            Text(
                pillLabel,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = accent,
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
    val healthyLow = PhStatus.HEALTHY_MIN.toFloat()
    val healthyHigh = PhStatus.HEALTHY_MAX.toFloat()
    val healthy = PhStatus.HEALTHY.color
    val elevated = PhStatus.ELEVATED.color
    val legacy = MaterialTheme.colorScheme.onSurfaceVariant

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        // Off-scale legacy urine values (up to 9.0) are clamped to the vaginal axis so they stay visible.
        fun yFor(v: Float) = h - ((v.coerceIn(min, max) - min) / (max - min)) * h

        // Two bands: healthy (3.8–4.5) and elevated (above 4.5). Provisional — see PhStatus.
        drawRect(healthy.copy(alpha = 0.10f), topLeft = Offset(0f, yFor(healthyHigh)), size = androidx.compose.ui.geometry.Size(w, yFor(healthyLow) - yFor(healthyHigh)))
        drawRect(elevated.copy(alpha = 0.06f), topLeft = Offset(0f, 0f), size = androidx.compose.ui.geometry.Size(w, yFor(healthyHigh)))

        val n = readings.size
        val points = readings.mapIndexed { i, r ->
            val x = if (n == 1) w / 2 else w * i / (n - 1)
            val isVaginal = r.measurementType == PhMeasurement.VAGINAL
            Pair(Offset(x, yFor(r.phValue.toFloat())), isVaginal)
        }
        // Connect the line only between consecutive vaginal readings — legacy points stay standalone.
        for (i in 0 until points.size - 1) {
            if (points[i].second && points[i + 1].second) {
                drawLine(
                    color = ElectricLavender,
                    start = points[i].first,
                    end = points[i + 1].first,
                    strokeWidth = 4f,
                    cap = StrokeCap.Round,
                )
            }
        }
        points.forEach { (p, isVaginal) ->
            if (isVaginal) {
                drawCircle(ElectricLavender, radius = 5f, center = p)
                drawCircle(Color.White, radius = 2f, center = p)
            } else {
                // Legacy urine reading: muted, hollow ring.
                drawCircle(legacy, radius = 4.5f, center = p, style = Stroke(width = 2f))
            }
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
