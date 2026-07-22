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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import android.content.res.Configuration
import com.genesyx.app.domain.content.phaseLabel
import com.genesyx.app.domain.cycle.CycleEngine
import com.genesyx.app.domain.model.CalendarCell
import com.genesyx.app.domain.model.CycleSettings
import com.genesyx.app.domain.model.DayType
import com.genesyx.app.domain.model.LogDay
import com.genesyx.app.domain.model.Phase
import com.genesyx.app.ui.components.CycleSettingsDialog
import com.genesyx.app.ui.components.DailyLogSummary
import com.genesyx.app.ui.components.Eyebrow
import com.genesyx.app.ui.components.GxPrimaryButton
import com.genesyx.app.ui.components.PhReadingRow
import com.genesyx.app.ui.components.tintOnWhite
import com.genesyx.app.ui.navigation.Screen
import com.genesyx.app.ui.theme.BabyLavender
import com.genesyx.app.ui.theme.ElectricBlue
import com.genesyx.app.ui.theme.ElectricLavender
import com.genesyx.app.ui.theme.GenesyxTheme
import com.genesyx.app.ui.theme.PhOptimal
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
    val settings by viewModel.settings.collectAsState()
    val logDays by viewModel.logDays.collectAsState()
    val summaries by viewModel.trackerSummaries.collectAsState()
    TrackContent(
        settings = settings,
        logDays = logDays,
        summaries = summaries,
        onNavigate = { navController.navigate(it) },
        onSaveCycle = { viewModel.saveCycleSettings(it) },
    )
}

@Composable
fun TrackContent(
    settings: CycleSettings?,
    logDays: Map<LocalDate, LogDay> = emptyMap(),
    summaries: TrackerSummaries = emptyTrackerSummaries(),
    onNavigate: (String) -> Unit,
    onSaveCycle: (CycleSettings) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val today = remember { LocalDate.now() }

    var monthAnchor by remember { mutableStateOf(YearMonth.now()) }
    var showCycleDialog by remember { mutableStateOf(false) }
    var selectedDay by remember { mutableStateOf<CalendarCell.Day?>(null) }

    // Settings arrive asynchronously, and editing them moves the earliest month. Pull the anchor
    // back in range rather than leaving it stranded on a month the calendar no longer covers.
    LaunchedEffect(settings) {
        settings?.let { monthAnchor = CycleEngine.clampMonth(monthAnchor, it, today) }
    }

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
                // The calendar only spans what is actually knowable: from the month of the last
                // recorded period (before it, there is no basis at all) to a few cycles ahead.
                // Unbounded nav let the user page to 2099 or to years before she ever logged, with
                // every day painted as confidently as today.
                val earliest = settings?.let { CycleEngine.earliestMonth(it) }
                val latest = remember(today) { CycleEngine.latestMonth(today) }
                val canGoBack = earliest != null && monthAnchor > earliest
                val canGoForward = monthAnchor < latest

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MonthNavButton(Icons.Filled.ChevronLeft, "Previous month", enabled = canGoBack) {
                        monthAnchor = monthAnchor.minusMonths(1)
                    }
                    Text(
                        monthAnchor.format(monthNavFormat),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                    )
                    MonthNavButton(Icons.Filled.ChevronRight, "Next month", enabled = canGoForward) {
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
                                        is CalendarCell.Day -> DayCell(cell, today) { selectedDay = cell }
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

        Spacer(Modifier.height(12.dp))

        // ── Current phase card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            val info = settings?.let { CycleEngine.getCyclePhase(it, today) }
            Column(Modifier.padding(20.dp)) {
                Eyebrow("Current phase", color = ElectricLavender)
                Spacer(Modifier.height(6.dp))
                Text(
                    info?.let { phaseLabel.getValue(it.phase) } ?: "—",
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 22.sp),
                    color = colors.onSurface,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = when {
                        info == null -> "Set up your cycle to see today's phase."
                        info.dayOfCycle in info.fertileWindow ->
                            "You're in your fertile window. Stay hydrated and prioritise rest."
                        else -> "About ${info.daysUntilNextPeriod} days until your next period."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // ── Add to today's log
        GxPrimaryButton(
            text = "Add to today's log",
            onClick = { onNavigate(Screen.Log.route) },
            leadingIcon = Icons.Filled.Add,
        )

        Spacer(Modifier.height(20.dp))

        // ── Your Trackers — the canonical entry into inspecting/editing each tracked signal.
        Eyebrow("Your trackers", color = colors.onSurfaceVariant)
        Spacer(Modifier.height(10.dp))
        YourTrackersCard(summaries = summaries, onNavigate = onNavigate)

        Spacer(Modifier.height(24.dp))
    }

    // ── Dialogs
    if (showCycleDialog) {
        CycleSettingsDialog(
            current = settings,
            onDismiss = { showCycleDialog = false },
            onSave = {
                onSaveCycle(it)
                showCycleDialog = false
            },
        )
    }

    selectedDay?.let { day ->
        DayDetailDialog(
            day = day,
            logDay = logDays[day.date],
            today = today,
            onDismiss = { selectedDay = null },
        )
    }
}

@Composable
private fun MonthNavButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    cd: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(colors.surfaceVariant.copy(alpha = if (enabled) 1f else 0.4f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            cd,
            tint = if (enabled) colors.onSurface else colors.onSurface.copy(alpha = 0.3f),
            modifier = Modifier.size(20.dp),
        )
    }
}

/**
 * A day of the cycle. Days after [today] are **predictions** from a fixed-length model, not
 * something the user recorded, so they are drawn faded and outlined rather than solid — the grid
 * used to render a projected ovulation day exactly as confidently as one that had already happened.
 */
@Composable
private fun DayCell(cell: CalendarCell.Day, today: LocalDate, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val type = CycleEngine.dayTypeFor(cell.info)
    val predicted = cell.date.isAfter(today)

    val base = when (type) {
        DayType.PERIOD -> PowderPink.tintOnWhite(0.55f)
        DayType.FERTILE -> PowderBlue.tintOnWhite(0.55f)
        DayType.OVULATION -> ElectricLavender
        DayType.LUTEAL -> BabyLavender.tintOnWhite(0.25f)
        DayType.FOLLICULAR -> colors.surface
    }
    val bg = if (predicted) base.copy(alpha = 0.3f) else base

    // White-on-lavender only reads on the solid ovulation fill; a faded one needs normal ink.
    val fg = when {
        predicted -> colors.onSurface.copy(alpha = 0.65f)
        type == DayType.OVULATION -> Color.White
        else -> colors.onSurface
    }
    val border = when {
        cell.isToday -> BorderStroke(2.dp, colors.onSurface)
        predicted -> BorderStroke(1.dp, colors.outline)
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
        "Fertile window" to PowderBlue.tintOnWhite(0.55f),
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
        Spacer(Modifier.height(8.dp))
        Text(
            "Faded days are predictions — they shift as you log.",
            fontSize = 11.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * [logDay] is what the user actually logged on [day] — null when nothing was. A future day has
 * nothing to show but the prediction; a past day shows its log, and only claims there isn't one
 * after the store has been asked.
 */
@Composable
private fun DayDetailDialog(
    day: CalendarCell.Day,
    logDay: LogDay?,
    today: LocalDate,
    onDismiss: () -> Unit,
) {
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
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Eyebrow("Day ${day.info.dayOfCycle} · ${phaseLabel.getValue(phase)}", color = ElectricLavender)
                Spacer(Modifier.height(8.dp))

                if (isFuture) {
                    Text(
                        text = when {
                            phase == Phase.OVULATORY -> "Predicted: ovulation day — peak fertility."
                            isFertile -> "Predicted: fertile window."
                            else -> "Predicted: ${phaseLabel.getValue(phase).lowercase()}."
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.onSurfaceVariant,
                    )
                    return@Column
                }

                val hasLog = logDay != null && !logDay.isEmpty
                if (!hasLog) {
                    Text(
                        "Nothing logged on this day.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.onSurfaceVariant,
                    )
                    return@Column
                }

                if (logDay!!.hasDailyContent) {
                    DailyLogSummary(logDay.dailyLog!!)
                }
                if (logDay.phReadings.isNotEmpty()) {
                    if (logDay.hasDailyContent) Spacer(Modifier.height(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        logDay.phReadings.forEach { PhReadingRow(it) }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = ElectricLavender) }
        },
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Your Trackers
// ─────────────────────────────────────────────────────────────────────────────

private data class TrackerRowSpec(
    val title: String,
    val icon: ImageVector,
    val tint: Color,
    val route: String,
    val summary: TrackerSummary,
)

@Composable
private fun YourTrackersCard(summaries: TrackerSummaries, onNavigate: (String) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val rows = listOf(
        TrackerRowSpec("Cycle", Icons.Outlined.CalendarMonth, ElectricLavender, Screen.CycleDetail.route, summaries.cycle),
        TrackerRowSpec("Hydration", Icons.Outlined.WaterDrop, ElectricBlue, Screen.HydrationDetail.route, summaries.hydration),
        TrackerRowSpec("Vaginal pH", Icons.Outlined.Science, PhOptimal, Screen.PhDetail.route, summaries.ph),
        TrackerRowSpec("Sleep", Icons.Outlined.Bedtime, BabyLavender, Screen.SleepDetail.route, summaries.sleep),
        TrackerRowSpec("Symptoms", Icons.Outlined.MonitorHeart, PowderPink, Screen.SymptomsDetail.route, summaries.symptoms),
        TrackerRowSpec("Nutrition", Icons.Outlined.Restaurant, PowderBlue, Screen.NutritionDetail.route, summaries.nutrition),
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column {
            rows.forEachIndexed { i, row ->
                TrackerRow(row, onClick = { onNavigate(row.route) })
                if (i < rows.lastIndex) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(start = 66.dp, end = 16.dp)
                            .height(1.dp)
                            .background(colors.outline.copy(alpha = 0.5f)),
                    )
                }
            }
        }
    }
}

@Composable
private fun TrackerRow(spec: TrackerRowSpec, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick)
            .clearAndSetSemantics { contentDescription = "${spec.title}. ${spec.summary.value}" }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(spec.tint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(spec.icon, null, tint = spec.tint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text(spec.title, style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
            Spacer(Modifier.height(2.dp))
            Text(
                spec.summary.value,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        }
        if (spec.summary.spark.isNotEmpty()) {
            SparkDots(spec.summary.spark, spec.tint)
            Spacer(Modifier.size(8.dp))
        }
        Icon(Icons.Filled.ChevronRight, null, tint = colors.onSurfaceVariant, modifier = Modifier.size(20.dp))
    }
}

/** Seven compact day dots — filled where that day has data, muted where it doesn't. */
@Composable
private fun SparkDots(spark: List<Boolean>, tint: Color) {
    val colors = MaterialTheme.colorScheme
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        spark.forEach { on ->
            Box(
                Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(if (on) tint else colors.onSurfaceVariant.copy(alpha = 0.25f)),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Compose Previews — TrackContent is stateless, so we feed it sample data.
// ─────────────────────────────────────────────────────────────────────────────

private val sampleTrackSettings = CycleSettings(
    lastPeriodDate = LocalDate.now().minusDays(8),
    cycleLength = 28,
    periodLength = 5,
)

@Preview(name = "Track — light", showBackground = true, showSystemUi = true)
@Composable
private fun TrackContentLightPreview() {
    GenesyxTheme(darkTheme = false) {
        TrackContent(settings = sampleTrackSettings, onNavigate = {}, onSaveCycle = {})
    }
}

@Preview(
    name = "Track — dark",
    showBackground = true,
    showSystemUi = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun TrackContentDarkPreview() {
    GenesyxTheme(darkTheme = true) {
        TrackContent(settings = sampleTrackSettings, onNavigate = {}, onSaveCycle = {})
    }
}

@Preview(name = "Track — not set up", showBackground = true, showSystemUi = true)
@Composable
private fun TrackContentEmptyPreview() {
    GenesyxTheme(darkTheme = false) {
        TrackContent(settings = null, onNavigate = {}, onSaveCycle = {})
    }
}
