package com.genesyx.app.ui.track

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.genesyx.app.domain.content.phaseLabel
import com.genesyx.app.domain.cycle.CycleEngine
import com.genesyx.app.domain.model.CalendarCell
import com.genesyx.app.domain.model.CycleSettings
import com.genesyx.app.domain.model.DayType
import com.genesyx.app.domain.model.Phase
import com.genesyx.app.domain.model.PhReading
import com.genesyx.app.ui.components.CycleSettingsDialog
import com.genesyx.app.ui.components.Eyebrow
import com.genesyx.app.ui.components.PhLogDialog
import com.genesyx.app.ui.components.PhTrackerCard
import com.genesyx.app.ui.components.tintOnWhite
import com.genesyx.app.ui.theme.BabyLavender
import com.genesyx.app.ui.theme.ElectricLavender
import com.genesyx.app.ui.theme.PowderBlue
import com.genesyx.app.ui.theme.PowderPink
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private val monthTitleFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")
private val monthNavFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM yyyy")
private val dayTitleFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, d MMM")

@Composable
fun TrackScreen(
    navController: NavController,
    viewModel: TrackViewModel = hiltViewModel(),
) {
    val colors = MaterialTheme.colorScheme
    val settings by viewModel.settings.collectAsState()
    val readings by viewModel.readings.collectAsState()
    val today = remember { LocalDate.now() }

    var monthAnchor by remember { mutableStateOf(YearMonth.now()) }
    var showCycleDialog by remember { mutableStateOf(false) }
    var showPhDialog by remember { mutableStateOf(false) }
    var editingReading by remember { mutableStateOf<PhReading?>(null) }
    var selectedDay by remember { mutableStateOf<CalendarCell.Day?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(20.dp))

        // ── Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    monthAnchor.format(monthTitleFormat),
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.onBackground,
                )
                Text(
                    text = settings?.let {
                        val info = CycleEngine.getCyclePhase(it, today)
                        val cycleNum = CycleEngine.cycleNumberFor(it.lastPeriodDate, it.cycleLength, today)
                        "Cycle $cycleNum · Day ${info.dayOfCycle}"
                    } ?: "Set up your cycle",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
            }
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(colors.surface)
                    .clickable { showCycleDialog = true },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Edit, "Edit cycle", tint = colors.onSurface, modifier = Modifier.size(18.dp))
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Calendar card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column(Modifier.padding(20.dp)) {
                // Month nav
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MonthNavButton(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous month") {
                        monthAnchor = monthAnchor.minusMonths(1)
                    }
                    Text(
                        monthAnchor.format(monthNavFormat),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                    )
                    MonthNavButton(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next month") {
                        monthAnchor = monthAnchor.plusMonths(1)
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Weekday header
                Row(Modifier.fillMaxWidth()) {
                    listOf("S", "M", "T", "W", "T", "F", "S").forEach { d ->
                        Text(
                            d,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant,
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                if (settings == null) {
                    EmptyCalendar { showCycleDialog = true }
                } else {
                    val grid = remember(monthAnchor, settings, today) {
                        CycleEngine.buildMonthGrid(monthAnchor, settings!!, today)
                    }
                    grid.chunked(7).forEach { week ->
                        Row(Modifier.fillMaxWidth()) {
                            week.forEach { cell ->
                                Box(Modifier.weight(1f).padding(2.dp)) {
                                    when (cell) {
                                        is CalendarCell.Empty -> Spacer(Modifier.aspectRatio(1f))
                                        is CalendarCell.Day -> DayCell(cell) { selectedDay = cell }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Legend()
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── pH Tracker card
        PhTrackerCard(
            readings = readings,
            onLogClick = {
                editingReading = null
                showPhDialog = true
            },
        )

        Spacer(Modifier.height(24.dp))
    }

    // ── Dialogs
    if (showCycleDialog) {
        CycleSettingsDialog(
            current = settings,
            onDismiss = { showCycleDialog = false },
            onSave = {
                viewModel.saveCycleSettings(it)
                showCycleDialog = false
            },
        )
    }

    if (showPhDialog) {
        PhLogDialog(
            existing = editingReading,
            onDismiss = { showPhDialog = false },
            onSave = { reading ->
                if (editingReading == null) viewModel.savePhReading(reading)
                else viewModel.updatePhReading(reading)
                showPhDialog = false
            },
            onDelete = { id ->
                viewModel.deletePhReading(id)
                showPhDialog = false
            },
        )
    }

    selectedDay?.let { day ->
        DayDetailDialog(day = day, today = today, onDismiss = { selectedDay = null })
    }
}

@Composable
private fun MonthNavButton(icon: androidx.compose.ui.graphics.vector.ImageVector, cd: String, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(colors.surfaceVariant)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, cd, tint = colors.onSurface, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun DayCell(cell: CalendarCell.Day, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val type = CycleEngine.dayTypeFor(cell.info)

    val bg = when (type) {
        DayType.PERIOD -> PowderPink.tintOnWhite(0.55f)
        DayType.FERTILE -> PowderBlue.tintOnWhite(0.55f)
        DayType.OVULATION -> ElectricLavender
        DayType.LUTEAL -> BabyLavender.tintOnWhite(0.25f)
        DayType.FOLLICULAR -> colors.surface
    }
    val fg = if (type == DayType.OVULATION) Color.White else colors.onSurface
    val border = when {
        cell.isToday -> BorderStroke(2.dp, colors.onSurface)
        type == DayType.FOLLICULAR -> BorderStroke(1.dp, colors.outline)
        else -> null
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .let { if (border != null) it.border(border, RoundedCornerShape(12.dp)) else it }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            cell.date.dayOfMonth.toString(),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = fg,
        )
    }
}

@Composable
private fun EmptyCalendar(onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, colors.outline, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Add your cycle", style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
        Spacer(Modifier.height(4.dp))
        Text(
            "Tell us when your last period started to see your phases here.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}

@Composable
private fun Legend() {
    val items = listOf(
        "Period" to PowderPink.tintOnWhite(0.55f),
        "Fertile" to PowderBlue.tintOnWhite(0.55f),
        "Ovulation" to ElectricLavender,
        "Luteal" to BabyLavender.tintOnWhite(0.25f),
    )
    Column {
        items.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                row.forEach { (label, color) ->
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(14.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(color),
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(label, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun DayDetailDialog(day: CalendarCell.Day, today: LocalDate, onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val phase = day.info.phase
    val isFuture = day.date.isAfter(today)
    val isFertile = day.info.dayOfCycle in day.info.fertileWindow

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = colors.surface,
        title = { Text(day.date.format(dayTitleFormat), style = MaterialTheme.typography.titleLarge, color = colors.onSurface) },
        text = {
            Column {
                Eyebrow("Day ${day.info.dayOfCycle} · ${phaseLabel.getValue(phase)}", color = ElectricLavender)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = when {
                        isFuture && phase == Phase.OVULATORY -> "Predicted: ovulation day — peak fertility."
                        isFuture && isFertile -> "Predicted: fertile window."
                        isFuture -> "Predicted: ${phaseLabel.getValue(phase).lowercase()}."
                        else -> "No log yet for this day."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = ElectricLavender) }
        },
    )
}
