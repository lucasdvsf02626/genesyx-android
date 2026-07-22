package com.genesyx.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.genesyx.app.domain.model.DailyLog
import com.genesyx.app.domain.model.EnergyLevel
import com.genesyx.app.domain.model.PhMeasurement
import com.genesyx.app.domain.model.PhReading
import com.genesyx.app.domain.ph.PhCopy
import com.genesyx.app.domain.ph.PhStatus
import java.time.format.DateTimeFormatter

/**
 * Renders what the user actually logged on a day. Shared by "My logs" and the Track calendar's
 * day-detail dialog so a day reads the same wherever it is opened — the calendar used to assert
 * "No log yet for this day" without ever reading the store.
 *
 * Only fields the user filled in are shown; a blank [DailyLog] renders nothing, so callers should
 * gate on [com.genesyx.app.domain.model.LogDay.hasDailyContent] before showing this.
 */
@Composable
fun DailyLogSummary(log: DailyLog, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        log.mood?.let { InfoRow("Mood", it.label) }
        log.energy?.let { InfoRow("Energy", it.label()) }
        if (log.symptoms.isNotEmpty()) InfoRow("Symptoms", log.symptoms.joinToString(", "))
        log.sleepMinutes?.let { InfoRow("Sleep", sleepLabel(it)) }
        if (log.supplements.isNotEmpty()) InfoRow("Supplements", log.supplements.joinToString(", "))
        if (log.waterMl > 0) InfoRow("Water", "%.1fL".format(log.waterMl / 1000f))
        log.notes?.takeIf { it.isNotBlank() }?.let { InfoRow("Notes", it) }
    }
}

/** One pH reading with its status and the time it was taken. */
@Composable
fun PhReadingRow(reading: PhReading) {
    val colors = MaterialTheme.colorScheme
    // Legacy urine readings are on a different scale, so they aren't classified Healthy/Elevated.
    val isLegacy = reading.measurementType == PhMeasurement.URINE
    val accent = if (isLegacy) colors.onSurfaceVariant else PhStatus.classify(reading.phValue).color
    val statusLabel = if (isLegacy) PhCopy.LEGACY_MARKER else PhStatus.classify(reading.phValue).label
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(accent))
        Spacer(Modifier.size(10.dp))
        Text(
            "pH %.1f".format(reading.phValue),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = colors.onSurface,
        )
        Spacer(Modifier.size(8.dp))
        Text(statusLabel, style = MaterialTheme.typography.bodyMedium, color = accent)
        Spacer(Modifier.weight(1f))
        Text(
            reading.recordedAt.format(TIME_FMT),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    val colors = MaterialTheme.colorScheme
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
            modifier = Modifier.width(96.dp),
        )
        Text(value, style = MaterialTheme.typography.bodyMedium, color = colors.onSurface)
    }
}

private val TIME_FMT = DateTimeFormatter.ofPattern("HH:mm")

private fun sleepLabel(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return if (m == 0) "${h}h" else "${h}h ${m}m"
}

private fun EnergyLevel.label(): String = id.replaceFirstChar { it.uppercase() }
