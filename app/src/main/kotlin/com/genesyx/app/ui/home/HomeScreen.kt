package com.genesyx.app.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import android.content.res.Configuration
import com.genesyx.app.R
import com.genesyx.app.domain.hydration.HydrationPace
import com.genesyx.app.domain.model.CycleSettings
import com.genesyx.app.domain.ph.PhCopy
import com.genesyx.app.ui.components.CycleSettingsDialog
import com.genesyx.app.ui.components.Eyebrow
import com.genesyx.app.ui.components.GxPrimaryButton
import com.genesyx.app.ui.components.HydrationStatusPill
import com.genesyx.app.ui.components.PastDatePickerDialog
import com.genesyx.app.ui.components.hydrationStatusLabel
import com.genesyx.app.ui.components.tintOnWhite
import com.genesyx.app.ui.navigation.Screen
import com.genesyx.app.ui.theme.BabyLavender
import com.genesyx.app.ui.theme.ElectricBlue
import com.genesyx.app.ui.theme.ElectricLavender
import com.genesyx.app.ui.theme.ElectricPink
import com.genesyx.app.ui.theme.PhOptimal
import com.genesyx.app.ui.theme.PowderBlue
import java.time.LocalDate

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    // "Select Track, then open the detail": the tab switch makes Track the surface underneath, so
    // Back from the detail lands on Track rather than Home.
    fun openTrackerDetail(route: String) {
        navController.navigate(Screen.Track.route) {
            popUpTo(Screen.Home.route) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
        navController.navigate(route)
    }
    HomeContent(
        state = state,
        onNavigate = { navController.navigate(it) },
        onOpenHydration = { openTrackerDetail(Screen.HydrationDetail.route) },
        onOpenPh = { openTrackerDetail(Screen.PhDetail.route) },
        onSaveCycle = { viewModel.saveCycleSettings(it) },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeContent(
    state: HomeUiState,
    onNavigate: (String) -> Unit,
    onOpenHydration: () -> Unit = {},
    onOpenPh: () -> Unit = {},
    onSaveCycle: (CycleSettings) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    var showCycleDialog by remember { mutableStateOf(false) }
    var cycleSeed by remember { mutableStateOf<CycleSettings?>(null) }
    var menuOpen by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(colors.background)) {
        if (colors.background.luminance() > 0.5f) {
            Image(
                painter = painterResource(R.drawable.home_hero_bg),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(20.dp))

            // ── Greeting header + avatar (44dp lavender→pink gradient)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(state.greeting, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                    Text(state.userName, style = MaterialTheme.typography.headlineMedium, color = colors.onBackground)
                }
                Box {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(BabyLavender, ElectricPink)))
                            .clickable { menuOpen = true },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            state.userName.firstOrNull()?.uppercase() ?: "G",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        if (!state.signedIn) {
                            DropdownMenuItem(
                                text = { Text("Sign in or create account") },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Login, null) },
                                onClick = { menuOpen = false; onNavigate(Screen.Auth.route) },
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Profile") },
                            leadingIcon = { Icon(Icons.Filled.Person, null) },
                            onClick = { menuOpen = false; onNavigate(Screen.Profile.route) },
                        )
                        DropdownMenuItem(
                            text = { Text("Cycle setup") },
                            leadingIcon = { Icon(Icons.Filled.Settings, null) },
                            onClick = { menuOpen = false; cycleSeed = state.settings; showCycleDialog = true },
                        )
                    }
                }
            }

            if (!state.signedIn) {
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(ElectricLavender)
                        .clickable { onNavigate(Screen.Auth.route) }
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Sign in to save your journey", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("Cycle setup, daily logs, and profile sync.", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                    }
                    Icon(Icons.AutoMirrored.Filled.Login, null, tint = Color.White)
                }
            }

            Spacer(Modifier.height(24.dp))

            if (state.cycleSetUp) {
                CycleHeroCard(state) { cycleSeed = state.settings; showCycleDialog = true }
                Spacer(Modifier.height(12.dp))
                TodayFocusCard(state)
            } else {
                // First-run: an honest setup card in place of the phase/focus content.
                FirstRunSetupCard(onStart = { seed ->
                    cycleSeed = seed
                    showCycleDialog = true
                })
            }

            Spacer(Modifier.height(12.dp))
            HydrationSummaryCard(state, onOpenHydration)

            if (com.genesyx.app.core.FeatureFlags.PH_TRACKING) {
                Spacer(Modifier.height(12.dp))
                PhNudgeCard(state.phLatest, state.phLatestIsLegacy, onOpenPh)
            }

            Spacer(Modifier.height(20.dp))
            GxPrimaryButton(text = "Log today", onClick = { onNavigate(Screen.Log.route) }, leadingIcon = Icons.Filled.Add)

            Spacer(Modifier.height(24.dp))
        }
    }

    if (showCycleDialog) {
        CycleSettingsDialog(
            current = cycleSeed ?: state.settings,
            onDismiss = { showCycleDialog = false },
            onSave = { onSaveCycle(it); showCycleDialog = false },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CycleHeroCard(state: HomeUiState, onEdit: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface.copy(alpha = 0.72f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        onClick = onEdit,
    ) {
        Column(Modifier.padding(24.dp)) {
            Text(state.cycleEyebrow, style = MaterialTheme.typography.labelSmall, color = ElectricLavender)
            Spacer(Modifier.height(10.dp))
            Text(state.cycleHeadline, style = MaterialTheme.typography.headlineMedium, color = colors.onSurface, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text(state.cycleSub, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            if (state.cycleTags.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    state.cycleTags.forEachIndexed { i, tag -> TagChip(tag, primary = i == 0) }
                }
            }
            // Divider + three metrics
            Spacer(Modifier.height(18.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(colors.outline.copy(alpha = 0.5f)))
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth()) {
                HeroMetric("Cycle day", state.cycleDay?.let { "Day $it" } ?: "—", Modifier.weight(1f))
                HeroMetric("Next period", state.daysToNextLabel ?: "—", Modifier.weight(1f))
                HeroMetric("Ovulation", state.ovulationDayLabel ?: "—", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun HeroMetric(label: String, value: String, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(modifier) {
        Eyebrow(label, color = colors.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
    }
}

@Composable
private fun TodayFocusCard(state: HomeUiState) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(20.dp)) {
            Eyebrow("Today's focus", color = colors.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            if (state.todayFocusTitle != null) {
                Text(state.todayFocusTitle, style = MaterialTheme.typography.titleLarge, color = colors.onSurface)
                Spacer(Modifier.height(4.dp))
                Text(state.todayFocusBody.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            } else {
                Text("Complete your cycle setup to see focus foods.", style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun HydrationSummaryCard(state: HomeUiState, onOpen: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val litres = state.hydrationLitres ?: 0f
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .clearAndSetSemantics {
                contentDescription = "Hydration. ${"%.1f".format(litres)} of ${"%.1f".format(state.hydrationGoalLitres)} litres. ${hydrationStatusLabel(state.hydrationPace)}. Opens hydration tracker."
            },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Eyebrow("Hydration", color = ElectricBlue)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Track", style = MaterialTheme.typography.bodyMedium, color = ElectricBlue, fontWeight = FontWeight.Medium)
                    Icon(Icons.Filled.ChevronRight, null, tint = ElectricBlue, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                HydrationRing(state.hydrationPercent)
                Spacer(Modifier.size(16.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("${state.hydrationLitres?.let { "%.1f".format(it) } ?: "0.0"} L", style = MaterialTheme.typography.titleLarge, color = colors.onSurface)
                        Spacer(Modifier.size(4.dp))
                        Text("/ ${"%.1f".format(state.hydrationGoalLitres)} L", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, modifier = Modifier.padding(bottom = 2.dp))
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusPill(state.hydrationPace)
                        if (state.hydrationStreak > 0) {
                            Spacer(Modifier.size(8.dp))
                            Icon(Icons.Filled.LocalFireDepartment, null, tint = ElectricPink, modifier = Modifier.size(14.dp))
                            Text("${state.hydrationStreak}d", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                        }
                    }
                }
            }
            state.hydrationCoaching?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                state.weekOnGoal.forEach { on ->
                    Box(
                        Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(if (on) ElectricBlue else colors.surfaceVariant.copy(alpha = 0.6f)),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("${state.daysOnGoal} of 7 days on goal", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        }
    }
}

@Composable
private fun HydrationRing(percent: Int) {
    val colors = MaterialTheme.colorScheme
    val track = colors.surfaceVariant
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(60.dp)) {
        Canvas(Modifier.size(60.dp)) {
            val stroke = 6.dp.toPx()
            val inset = stroke / 2
            val arcSize = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke)
            val topLeft = androidx.compose.ui.geometry.Offset(inset, inset)
            drawArc(color = track, startAngle = -90f, sweepAngle = 360f, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            drawArc(
                brush = Brush.verticalGradient(listOf(ElectricBlue, PowderBlue)),
                startAngle = -90f,
                sweepAngle = 360f * (percent.coerceIn(0, 100) / 100f),
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
        Text("$percent%", style = MaterialTheme.typography.labelMedium, color = colors.onSurface, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun StatusPill(pace: HydrationPace) {
    HydrationStatusPill(pace)
}

@Composable
private fun PhNudgeCard(latest: Double?, isLegacy: Boolean, onOpen: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    // A pre-migration urine reading is on a different scale, so it's marked legacy, never shown as a
    // current vaginal reading. "Check your pH" is measurement-neutral and stays.
    val body = when {
        latest == null -> "Log today's reading in the pH tracker"
        isLegacy -> "Last reading %.1f · ${PhCopy.LEGACY_MARKER} — tap to log a vaginal reading".format(latest)
        else -> "Last reading %.1f — tap to log again".format(latest)
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .clearAndSetSemantics {
                contentDescription = "Check your pH. " + when {
                    latest == null -> "No reading yet"
                    isLegacy -> "Last reading %.1f, ${PhCopy.LEGACY_MARKER}".format(latest)
                    else -> "Last reading %.1f".format(latest)
                } + ". Opens pH tracker."
            },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(PhOptimal.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Outlined.Science, null, tint = PhOptimal, modifier = Modifier.size(20.dp)) }
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Check your pH", style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
                Text(
                    body,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
            Icon(Icons.Filled.ChevronRight, null, tint = colors.onSurfaceVariant, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun FirstRunSetupCard(onStart: (CycleSettings) -> Unit) {
    val colors = MaterialTheme.colorScheme
    var lastPeriod by remember { mutableStateOf<LocalDate?>(null) }
    var showPicker by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(24.dp)) {
            Text("Welcome to Genesyx", style = MaterialTheme.typography.headlineSmall, color = colors.onSurface, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text(
                "When did your last period start? Next we'll confirm your cycle length — every prediction is built from it.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surfaceVariant.copy(alpha = 0.5f))
                    .clickable { showPicker = true }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    lastPeriod?.let { "Last period: ${it.format(java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy"))}" } ?: "Choose last period date",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (lastPeriod != null) colors.onSurface else colors.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(16.dp))
            GxPrimaryButton(
                text = "Start tracking",
                enabled = lastPeriod != null,
                onClick = {
                    lastPeriod?.let { onStart(CycleSettings(lastPeriodDate = it)) }
                },
            )
        }
    }

    if (showPicker) {
        PastDatePickerDialog(
            initial = lastPeriod ?: LocalDate.now(),
            onDismiss = { showPicker = false },
            onPick = { lastPeriod = it; showPicker = false },
        )
    }
}

@Composable
private fun TagChip(text: String, primary: Boolean) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (primary) ElectricLavender.tintOnWhite(0.08f) else PowderBlue.tintOnWhite(0.22f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = text,
            fontSize = 11.5.sp,
            color = if (primary) ElectricLavender else colors.onSurface.copy(alpha = 0.75f),
            fontWeight = FontWeight.Medium,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Previews
// ─────────────────────────────────────────────────────────────────────────────

private val sampleHomeState = HomeUiState(
    userName = "Lucas",
    signedIn = true,
    greeting = "Good afternoon",
    settings = CycleSettings(lastPeriodDate = LocalDate.now().minusDays(8)),
    cycleSetUp = true,
    cycleEyebrow = "DAY 9 · FOLLICULAR",
    cycleHeadline = "Your body is preparing",
    cycleSub = "Energy is climbing toward your fertile window.",
    cycleTags = listOf("Follicular", "Rising energy", "Prep foods"),
    cycleDay = 9,
    daysToNextLabel = "19 days",
    ovulationDayLabel = "Day 14",
    todayFocusTitle = "Leafy greens & folate",
    todayFocusBody = "Spinach, lentils and citrus support the follicular build-up.",
    hydrationLitres = 1.6f,
    hydrationPercent = 67,
    hydrationPace = HydrationPace.ON_TRACK,
    hydrationStreak = 4,
    weekOnGoal = listOf(true, true, false, true, false, false, false),
    daysOnGoal = 3,
    hydrationCoaching = "This afternoon you're right on pace, about 800ml to go.",
    phLatest = 4.2,
    streakDays = 4,
)

@Preview(name = "Home — light", showBackground = true, showSystemUi = true)
@Composable
private fun HomeContentLightPreview() {
    com.genesyx.app.ui.theme.GenesyxTheme(darkTheme = false) {
        HomeContent(state = sampleHomeState, onNavigate = {}, onSaveCycle = {})
    }
}

@Preview(name = "Home — dark", showBackground = true, showSystemUi = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HomeContentDarkPreview() {
    com.genesyx.app.ui.theme.GenesyxTheme(darkTheme = true) {
        HomeContent(state = sampleHomeState, onNavigate = {}, onSaveCycle = {})
    }
}

@Preview(name = "Home — first run", showBackground = true, showSystemUi = true)
@Composable
private fun HomeContentEmptyPreview() {
    com.genesyx.app.ui.theme.GenesyxTheme(darkTheme = false) {
        HomeContent(state = HomeUiState(), onNavigate = {}, onSaveCycle = {})
    }
}
