package com.genesyx.app.ui.history

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.genesyx.app.domain.model.DailyLog
import com.genesyx.app.domain.model.EnergyLevel
import com.genesyx.app.domain.model.LogDay
import com.genesyx.app.domain.model.Mood
import com.genesyx.app.domain.model.PhReading
import com.genesyx.app.domain.ph.PhStatus
import com.genesyx.app.ui.components.ScreenHeader
import com.genesyx.app.ui.theme.GenesyxTheme
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun LogHistoryScreen(
    onBack: () -> Unit,
    viewModel: LogHistoryViewModel = hiltViewModel(),
) {
    val days by viewModel.days.collectAsState()
    LogHistoryContent(days = days, onBack = onBack)
}

@Composable
private fun LogHistoryContent(days: List<LogDay>, onBack: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier.fillMaxSize().background(colors.background),
    ) {
        ScreenHeader(
            title = "My logs",
            subtitle = "Everything you've tracked, newest first.",
            onBack = onBack,
            large = true,
        )
        if (days.isEmpty()) {
            EmptyState()
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(days, key = { it.date.toString() }) { day -> LogDayCard(day) }
            }
        }
    }
}

@Composable
private fun LogDayCard(day: LogDay) {
    val colors = MaterialTheme.colorScheme
    val today = remember { LocalDate.now() }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                dateLabel(day.date, today),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurface,
            )
            Spacer(Modifier.height(12.dp))

            day.dailyLog?.takeIf { day.hasDailyContent }?.let { log ->
                DailyLogBlock(log)
                if (day.phReadings.isNotEmpty()) Spacer(Modifier.height(12.dp))
            }

            day.phReadings.forEachIndexed { i, reading ->
                if (i > 0) Spacer(Modifier.height(8.dp))
                PhReadingRow(reading)
            }
        }
    }
}

@Composable
private fun DailyLogBlock(log: DailyLog) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        log.mood?.let { InfoRow("Mood", it.label) }
        log.energy?.let { InfoRow("Energy", it.label()) }
        if (log.symptoms.isNotEmpty()) InfoRow("Symptoms", log.symptoms.joinToString(", "))
        log.sleepMinutes?.let { InfoRow("Sleep", sleepLabel(it)) }
        if (log.supplements.isNotEmpty()) InfoRow("Supplements", log.supplements.joinToString(", "))
        if (log.waterMl > 0) InfoRow("Water", "%.1fL".format(log.waterMl / 1000f))
        log.notes?.takeIf { it.isNotBlank() }?.let { InfoRow("Notes", it) }
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

@Composable
private fun PhReadingRow(reading: PhReading) {
    val colors = MaterialTheme.colorScheme
    val status = PhStatus.classify(reading.phValue)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(status.color))
        Spacer(Modifier.size(10.dp))
        Text(
            "pH %.1f".format(reading.phValue),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = colors.onSurface,
        )
        Spacer(Modifier.size(8.dp))
        Text(status.label, style = MaterialTheme.typography.bodyMedium, color = status.color)
        Spacer(Modifier.weight(1f))
        Text(
            reading.recordedAt.format(TIME_FMT),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun EmptyState() {
    val colors = MaterialTheme.colorScheme
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "No logs yet",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = colors.onBackground,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Start tracking your day and pH from Home — your entries will appear here.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private val TIME_FMT = DateTimeFormatter.ofPattern("HH:mm")
private val DATE_FMT = DateTimeFormatter.ofPattern("EEE d MMM")

private fun dateLabel(date: LocalDate, today: LocalDate): String = when (date) {
    today -> "Today"
    today.minusDays(1) -> "Yesterday"
    else -> date.format(DATE_FMT)
}

private fun sleepLabel(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return if (m == 0) "${h}h" else "${h}h ${m}m"
}

private fun EnergyLevel.label(): String = id.replaceFirstChar { it.uppercase() }

// ── Previews ──────────────────────────────────────────────────────────────
private val previewDays = listOf(
    LogDay(
        date = LocalDate.now(),
        dailyLog = DailyLog(
            mood = Mood.GOOD,
            energy = EnergyLevel.NORMAL,
            symptoms = setOf("cramps", "headache"),
            sleepMinutes = 450,
            supplements = setOf("Folate"),
            notes = "Felt steady today.",
            waterMl = 1800,
        ),
        phReadings = listOf(
            PhReading(phValue = 6.5, recordedAt = LocalDateTime.now().withHour(9).withMinute(3)),
            PhReading(phValue = 7.8, recordedAt = LocalDateTime.now().withHour(20).withMinute(41)),
        ),
    ),
    LogDay(
        date = LocalDate.now().minusDays(1),
        dailyLog = DailyLog(waterMl = 1200),
        phReadings = emptyList(),
    ),
)

@Preview(name = "History — light", showBackground = true)
@Composable
private fun LogHistoryLightPreview() {
    GenesyxTheme(darkTheme = false) { LogHistoryContent(days = previewDays, onBack = {}) }
}

@Preview(name = "History — dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun LogHistoryDarkPreview() {
    GenesyxTheme(darkTheme = true) { LogHistoryContent(days = previewDays, onBack = {}) }
}

@Preview(name = "History — empty", showBackground = true)
@Composable
private fun LogHistoryEmptyPreview() {
    GenesyxTheme(darkTheme = false) { LogHistoryContent(days = emptyList(), onBack = {}) }
}
