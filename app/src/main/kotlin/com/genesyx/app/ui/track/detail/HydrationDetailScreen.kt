package com.genesyx.app.ui.track.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.genesyx.app.ui.components.Eyebrow
import com.genesyx.app.ui.components.HydrationGoalDialog
import com.genesyx.app.ui.components.HydrationStatusPill
import com.genesyx.app.ui.theme.ElectricBlue
import com.genesyx.app.ui.theme.ElectricLavender
import com.genesyx.app.ui.theme.PowderBlue
import java.time.format.DateTimeFormatter
import java.util.Locale

private val historyDay: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d MMM", Locale.UK)
private val weekdayLabels = listOf("M", "T", "W", "T", "F", "S", "S")

@Composable
fun HydrationDetailScreen(
    onBack: () -> Unit,
    viewModel: HydrationDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val colors = MaterialTheme.colorScheme
    var manual by remember { mutableStateOf("") }
    var editGoal by remember { mutableStateOf(false) }

    TrackerDetailScaffold(title = "Hydration", onBack = onBack) {
        Spacer(Modifier.height(8.dp))

        // ── Current amount, progress, status, coaching
        DetailCard {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("%.1f".format(state.waterMl / 1000f), fontSize = 34.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurface)
                Spacer(Modifier.size(6.dp))
                Text("/ ${"%.1f".format(state.goalMl / 1000f)} L", style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant, modifier = Modifier.padding(bottom = 4.dp))
                Spacer(Modifier.weight(1f))
                HydrationStatusPill(state.pace, modifier = Modifier.padding(bottom = 4.dp))
            }
            Spacer(Modifier.height(14.dp))
            LinearProgressIndicator(
                progress = { (state.waterMl.toFloat() / state.goalMl).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                color = ElectricBlue,
                trackColor = colors.surfaceVariant,
            )
            if (state.coaching.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(state.coaching, style = MaterialTheme.typography.bodyMedium, color = colors.onSurface.copy(alpha = 0.8f))
            }
        }

        Spacer(Modifier.height(12.dp))

        // ── Quick-add + manual entry
        DetailCard {
            Eyebrow("Add water", color = colors.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickAdd("-200", Modifier.weight(1f)) { viewModel.add(-200) }
                QuickAdd("+200", Modifier.weight(1f)) { viewModel.add(200) }
                QuickAdd("+300", Modifier.weight(1f)) { viewModel.add(300) }
                QuickAdd("+500", Modifier.weight(1f)) { viewModel.add(500) }
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = manual,
                    onValueChange = { new -> manual = new.filter(Char::isDigit).take(5) },
                    label = { Text("Set exact ml") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.size(8.dp))
                TextButton(
                    onClick = {
                        manual.toIntOrNull()?.let { viewModel.setWater(it) }
                        manual = ""
                    },
                    enabled = manual.toIntOrNull() != null,
                ) { Text("Set", color = ElectricLavender, fontWeight = FontWeight.SemiBold) }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "Changes save automatically to your log and sync when you're online.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(12.dp))

        // ── This week
        DetailCard {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Text("This week", style = MaterialTheme.typography.titleLarge, color = colors.onSurface)
                TextButton(onClick = { editGoal = true }) {
                    Text("Edit goal", style = MaterialTheme.typography.bodyMedium, color = ElectricLavender)
                }
            }
            Spacer(Modifier.height(12.dp))
            WeekBars(state.bars)
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricTile("7-day avg", state.avgMlPerDay?.let { "${it}ml" } ?: "—", Modifier.weight(1f))
                MetricTile("Days on goal", "${state.daysOnGoal}/7", Modifier.weight(1f))
                MetricTile("Streak", if (state.streakDays > 0) "${state.streakDays}d" else "—", Modifier.weight(1f))
            }
            if (state.insight.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                Text(state.insight, style = MaterialTheme.typography.bodyMedium, color = colors.onSurface.copy(alpha = 0.8f))
            }
        }

        Spacer(Modifier.height(12.dp))

        // ── Daily history
        DetailCard {
            Text("Daily total", style = MaterialTheme.typography.titleLarge, color = colors.onSurface)
            Spacer(Modifier.height(12.dp))
            state.history.forEach { (date, ml) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(date.format(historyDay), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                    Text(
                        if (ml > 0) "${ml}ml" else "—",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (ml > 0) colors.onSurface else colors.onSurfaceVariant,
                        fontWeight = if (ml > 0) FontWeight.Medium else FontWeight.Normal,
                    )
                }
            }
        }

        Spacer(Modifier.height(32.dp))
    }

    if (editGoal) {
        HydrationGoalDialog(
            current = state.goalMl,
            onDismiss = { editGoal = false },
            onSave = { viewModel.setGoal(it); editGoal = false },
        )
    }
}

@Composable
private fun DetailCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(20.dp), content = content)
    }
}

@Composable
private fun QuickAdd(label: String, modifier: Modifier, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(ElectricBlue.copy(alpha = 0.14f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium, color = ElectricBlue, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun WeekBars(bars: List<Int>) {
    val colors = MaterialTheme.colorScheme
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().height(96.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            bars.forEach { v ->
                // An unlogged day is a flat grey track, never a blue minimum bar.
                if (v <= 0) {
                    Box(Modifier.weight(1f).height(2.dp).clip(CircleShape).background(colors.onSurfaceVariant.copy(alpha = 0.25f)))
                } else {
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight((v / 100f).coerceIn(0.02f, 1f))
                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                            .background(Brush.verticalGradient(listOf(ElectricBlue, PowderBlue))),
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            weekdayLabels.forEach {
                Text(it, modifier = Modifier.weight(1f), fontSize = 10.sp, color = colors.onSurfaceVariant, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun MetricTile(label: String, value: String, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier.clip(RoundedCornerShape(16.dp)).background(colors.surfaceVariant.copy(alpha = 0.4f)).padding(14.dp),
    ) {
        Eyebrow(label, color = colors.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.titleLarge, color = colors.onSurface)
    }
}
