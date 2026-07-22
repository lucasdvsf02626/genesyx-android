package com.genesyx.app.ui.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.genesyx.app.domain.model.DayType
import com.genesyx.app.ui.components.Eyebrow
import com.genesyx.app.ui.components.ScreenHeader
import com.genesyx.app.ui.components.tintOnWhite
import com.genesyx.app.ui.navigation.Screen
import com.genesyx.app.ui.theme.BabyLavender
import com.genesyx.app.ui.theme.ElectricBlue
import com.genesyx.app.ui.theme.ElectricLavender
import com.genesyx.app.ui.theme.PowderBlue
import com.genesyx.app.ui.theme.PowderPink
import java.time.format.DateTimeFormatter
import java.util.Locale

private val weekdayLabels = listOf("M", "T", "W", "T", "F", "S", "S")

/** "24 Jul" — British order, and short enough to sit two-up in a tile. */
private val dayMonth: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.UK)

@Composable
fun InsightsScreen(
    navController: NavController,
    viewModel: InsightsViewModel = hiltViewModel(),
) {
    val colors = MaterialTheme.colorScheme
    val weekly by viewModel.weeklySummary.collectAsState()
    val ph by viewModel.phInsights.collectAsState()
    val consistency by viewModel.consistencyInsights.collectAsState()
    val hydration by viewModel.hydrationInsights.collectAsState()
    val supplements by viewModel.supplementInsights.collectAsState()
    val sleep by viewModel.sleepInsights.collectAsState()
    val cycleRegularity by viewModel.cycleRegularityInsights.collectAsState()
    val symptoms by viewModel.symptomInsights.collectAsState()
    val ovulation by viewModel.ovulationInsights.collectAsState()

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
            LogHistoryEntryCard {
                navController.navigate(Screen.LogHistory.route)
            }

            Spacer(Modifier.height(12.dp))
            WeeklySummaryCard(weekly)

            Spacer(Modifier.height(12.dp))
            ConsistencyCard(consistency)

            Spacer(Modifier.height(12.dp))
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
            HydrationCard(hydration)

            // Directly after Hydration: the two nutrition signals sit together.
            Spacer(Modifier.height(12.dp))
            SupplementCard(supplements)

            Spacer(Modifier.height(12.dp))
            SleepCard(sleep)

            // The two cycle cards close the screen, both reading the one saved setup.
            Spacer(Modifier.height(12.dp))
            CycleRegularityCard(cycleRegularity)

            Spacer(Modifier.height(12.dp))
            SymptomPatternsCard(symptoms)

            Spacer(Modifier.height(12.dp))
            OvulationCard(ovulation)

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun LogHistoryEntryCard(onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("My logs", style = MaterialTheme.typography.titleLarge, color = colors.onSurface)
                Spacer(Modifier.height(2.dp))
                Text(
                    "See everything you've tracked",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = ElectricLavender,
            )
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
                Text("Vaginal pH", style = MaterialTheme.typography.titleLarge, color = colors.onSurface)
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

            // How much data the trend rests on, so a two-reading average doesn't read as a verdict.
            if (ph.readings30 > 0) {
                Spacer(Modifier.height(10.dp))
                Text(
                    if (ph.readings30 == 1) "Based on 1 reading in the last 30 days."
                    else "Based on ${ph.readings30} readings in the last 30 days.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
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
private fun WeeklySummaryCard(state: WeeklySummaryInsights) {
    val colors = MaterialTheme.colorScheme
    if (!state.hasData) {
        EmptyInsightsCard("Weekly summary", state.insight)
        return
    }

    InsightsCard {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Text("Weekly summary", style = MaterialTheme.typography.titleLarge, color = colors.onSurface)
            Text("This week", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        }

        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text("${state.daysLogged}", fontSize = 34.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurface)
            Spacer(Modifier.size(6.dp))
            Text("of 7 days logged", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, modifier = Modifier.padding(bottom = 6.dp))
            if (state.prevDaysLogged > 0) {
                Spacer(Modifier.weight(1f))
                Text(
                    "vs ${state.prevDaysLogged} last week",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ElectricLavender,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }
        }

        if (state.moodEnergyLine.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text(state.moodEnergyLine, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        }

        // Deltas against last week, only for metrics both weeks can support — see WeeklySummaryLogic.
        val deltas = buildList {
            state.hydrationDeltaMlPerDay?.let { add("Hydration" to signedMl(it)) }
            state.sleepDeltaMinutes?.let { add("Sleep" to signedDuration(it)) }
            state.supplementDaysDelta?.let { add("Supplements" to signedDays(it)) }
        }
        if (deltas.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                deltas.forEach { (label, value) ->
                    TextTile("$label vs last wk", value, Modifier.weight(1f))
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        Text(state.insight, style = MaterialTheme.typography.bodyMedium, color = colors.onSurface.copy(alpha = 0.8f))
    }
}

/** Signed, human deltas for the weekly-summary tiles. A flat delta reads "level", never "±0". */
private fun signedMl(delta: Int): String = when {
    delta > 0 -> "+${delta}ml"
    delta < 0 -> "-${-delta}ml"
    else -> "level"
}

private fun signedDuration(delta: Int): String = when {
    delta > 0 -> "+${SleepInsightLogic.formatDuration(delta)}"
    delta < 0 -> "-${SleepInsightLogic.formatDuration(-delta)}"
    else -> "level"
}

private fun signedDays(delta: Int): String = when {
    delta > 0 -> "+$delta ${if (delta == 1) "day" else "days"}"
    delta < 0 -> "-${-delta} ${if (delta == -1) "day" else "days"}"
    else -> "level"
}

@Composable
private fun ConsistencyCard(state: ConsistencyInsights) {
    val colors = MaterialTheme.colorScheme
    InsightsCard {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Text("Consistency", style = MaterialTheme.typography.titleLarge, color = colors.onSurface)
            if (state.daysLoggedThisWeek > 0) {
                Text(
                    "${state.daysLoggedThisWeek} of 7 days",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ElectricLavender,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        Spacer(Modifier.height(18.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            state.weekActivity.forEachIndexed { i, logged ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(if (logged) ElectricLavender else colors.surfaceVariant.copy(alpha = 0.5f)),
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(weekdayLabels[i], fontSize = 10.sp, color = colors.onSurfaceVariant)
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // "Hydration", not "Daily" — this tile counts water only, and Home's "Streak" counts any
            // logged activity. Two different numbers; they must not both be called a daily streak.
            StreakTile("Hydration", state.dailyStreak, "d", Modifier.weight(1f))
            StreakTile("Weekly streak", state.weeklyStreak, "w", Modifier.weight(1f))
            StreakTile("Best", state.bestDailyStreak, "d", Modifier.weight(1f))
        }

        Spacer(Modifier.height(14.dp))
        Text(state.insight, style = MaterialTheme.typography.bodyMedium, color = colors.onSurface.copy(alpha = 0.8f))
    }
}

@Composable
private fun StreakTile(label: String, value: Int, unit: String, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier.clip(RoundedCornerShape(16.dp)).background(colors.surfaceVariant.copy(alpha = 0.4f)).padding(14.dp),
    ) {
        Eyebrow(label, color = colors.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Text(
            if (value > 0) "$value$unit" else "—",
            style = MaterialTheme.typography.titleLarge,
            color = colors.onSurface,
        )
    }
}

@Composable
private fun HydrationCard(state: HydrationInsights) {
    val colors = MaterialTheme.colorScheme
    if (!state.hasData) {
        InsightsCard {
            Text("Hydration", style = MaterialTheme.typography.titleLarge, color = colors.onSurface)
            Spacer(Modifier.height(12.dp))
            Text(state.insight, style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
        }
        return
    }
    BarsCard(
        title = "Hydration",
        trailing = state.deltaMlPerDay?.let { if (it >= 0) "+${it}ml/day" else "${it}ml/day" },
        values = state.bars,
        labels = weekdayLabels,
        barHeight = 112.dp,
        brush = Brush.verticalGradient(listOf(ElectricBlue, PowderBlue)),
        insight = state.insight,
    )
}

@Composable
private fun SupplementCard(state: SupplementInsights) {
    val colors = MaterialTheme.colorScheme

    // Neither of these is a failure state, so neither reads like one.
    if (!state.hasPlan || !state.hasData) {
        InsightsCard {
            Text("Nutrition consistency", style = MaterialTheme.typography.titleLarge, color = colors.onSurface)
            Spacer(Modifier.height(12.dp))
            Text(
                if (state.hasPlan) state.insight else "Choose the supplements you're taking and this card will follow along.",
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurfaceVariant,
            )
        }
        return
    }

    Column {
        BarsCard(
            title = "Nutrition consistency",
            trailing = "of ${state.planSize} a day",
            values = state.bars,
            labels = weekdayLabels,
            barHeight = 112.dp,
            brush = Brush.verticalGradient(listOf(ElectricLavender, PowderBlue)),
            insight = state.insight,
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StreakTile("Days logged", state.daysLogged, "/7", Modifier.weight(1f))
            StreakTile("Supplements taken", state.suppTotal, "", Modifier.weight(1f))
        }
    }
}

@Composable
private fun SleepCard(state: SleepInsights) {
    val colors = MaterialTheme.colorScheme
    if (!state.hasData) {
        EmptyInsightsCard("Sleep", state.insight)
        return
    }

    Column {
        // No goal line, and no trailing figure: there is no "right" amount of sleep to score her
        // against, so the card shows the week and the average and makes no judgement.
        BarsCard(
            title = "Sleep",
            trailing = null,
            values = state.bars,
            labels = weekdayLabels,
            barHeight = 112.dp,
            brush = Brush.verticalGradient(listOf(BabyLavender, PowderBlue)),
            insight = state.insight,
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextTile(
                "Nightly average",
                SleepInsightLogic.formatDuration(state.nightlyAverageMinutes ?: 0),
                Modifier.weight(1f),
            )
            TextTile("Nights logged", "${state.nightsLogged}/7", Modifier.weight(1f))
        }
    }
}

@Composable
private fun CycleRegularityCard(state: CycleRegularityInsights) {
    val colors = MaterialTheme.colorScheme
    if (!state.hasData) {
        EmptyInsightsCard("Cycle regularity", state.insight)
        return
    }

    val length = state.cycleLength!!
    InsightsCard {
        Text("Cycle regularity", style = MaterialTheme.typography.titleLarge, color = colors.onSurface)

        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextTile("Your cycle", "$length days", Modifier.weight(1f))
            TextTile(
                "Typical",
                "${CycleRegularityLogic.typicalMin}–${CycleRegularityLogic.typicalMax} days",
                Modifier.weight(1f),
            )
        }

        // One point against a range — NOT a trend line. The app stores a single cycle length and no
        // history at all, so there is nothing here that could honestly be plotted over time.
        Spacer(Modifier.height(16.dp))
        val position = ((length - CycleRegularityLogic.typicalMin).toFloat() /
            (CycleRegularityLogic.typicalMax - CycleRegularityLogic.typicalMin))
            .coerceIn(0f, 1f)
        Box(Modifier.fillMaxWidth()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .align(Alignment.Center)
                    .clip(CircleShape)
                    .background(
                        if (state.inTypicalRange) PowderBlue.copy(alpha = 0.35f)
                        else colors.surfaceVariant.copy(alpha = 0.6f),
                    ),
            )
            Row(Modifier.fillMaxWidth()) {
                // Weights must be strictly positive, so a marker at either end still lays out.
                Spacer(Modifier.weight(position.coerceIn(0.001f, 0.999f)))
                Box(
                    Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(if (state.inTypicalRange) ElectricLavender else PowderPink),
                )
                Spacer(Modifier.weight((1f - position).coerceIn(0.001f, 0.999f)))
            }
        }

        Spacer(Modifier.height(14.dp))
        Text(state.insight, style = MaterialTheme.typography.bodyMedium, color = colors.onSurface.copy(alpha = 0.8f))
    }
}

@Composable
private fun SymptomPatternsCard(state: SymptomPatternInsights) {
    val colors = MaterialTheme.colorScheme
    InsightsCard {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Text("Symptom patterns", style = MaterialTheme.typography.titleLarge, color = colors.onSurface)
            Text("Last 28 days", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        }

        // The grid is drawn even when empty — an empty grid is the truth, and it shows her the shape
        // the card will take. It is never seeded with anything she didn't log.
        Spacer(Modifier.height(16.dp))
        state.heatmapValues.chunked(7).forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                week.forEach { count ->
                    Box(
                        Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                when {
                                    count <= 0 -> colors.surfaceVariant.copy(alpha = 0.45f)
                                    count == 1 -> ElectricLavender.copy(alpha = 0.35f)
                                    count == 2 -> ElectricLavender.copy(alpha = 0.6f)
                                    else -> ElectricLavender.copy(alpha = 0.9f)
                                },
                            ),
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(state.insight, style = MaterialTheme.typography.bodyMedium, color = colors.onSurface.copy(alpha = 0.8f))
    }
}

@Composable
private fun OvulationCard(state: OvulationInsights) {
    val colors = MaterialTheme.colorScheme
    if (!state.hasData) {
        EmptyInsightsCard("Ovulation", state.insight)
        return
    }

    InsightsCard {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Text("Ovulation", style = MaterialTheme.typography.titleLarge, color = colors.onSurface)
            // The card's headline word. Everything below it is arithmetic on her last period date.
            Text("Predicted", style = MaterialTheme.typography.bodyMedium, color = ElectricLavender, fontWeight = FontWeight.Medium)
        }

        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextTile("Ovulation day", state.ovulationDate?.format(dayMonth) ?: "—", Modifier.weight(1f))
            TextTile(
                "Fertile window",
                if (state.fertileWindowStart != null && state.fertileWindowEnd != null) {
                    "${state.fertileWindowStart.format(dayMonth)} – ${state.fertileWindowEnd.format(dayMonth)}"
                } else {
                    "—"
                },
                Modifier.weight(1f),
            )
        }

        // The cycle as a ribbon, coloured by the same DayType the Track calendar uses. Today is the
        // tall segment — a position in the cycle, not a measurement of it.
        Spacer(Modifier.height(18.dp))
        Row(
            modifier = Modifier.fillMaxWidth().height(28.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            state.dayTypes.forEachIndexed { index, type ->
                val isToday = index + 1 == state.currentDayOfCycle
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight(if (isToday) 1f else 0.6f)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            when (type) {
                                DayType.PERIOD -> PowderPink
                                DayType.FERTILE -> PowderBlue
                                DayType.OVULATION -> ElectricLavender
                                DayType.LUTEAL -> BabyLavender.copy(alpha = 0.45f)
                                DayType.FOLLICULAR -> colors.surfaceVariant
                            },
                        ),
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            "Day 1 to day ${state.cycleLength} — today is the tall marker.",
            fontSize = 10.sp,
            color = colors.onSurfaceVariant,
        )

        Spacer(Modifier.height(14.dp))
        Text(state.insight, style = MaterialTheme.typography.bodyMedium, color = colors.onSurface.copy(alpha = 0.8f))
    }
}

/** The one empty-state shape: the card's real title, and a line that says plainly what is missing. */
@Composable
private fun EmptyInsightsCard(title: String, message: String) {
    val colors = MaterialTheme.colorScheme
    InsightsCard {
        Text(title, style = MaterialTheme.typography.titleLarge, color = colors.onSurface)
        Spacer(Modifier.height(12.dp))
        Text(message, style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
    }
}

/** [StreakTile]'s sibling, for values that are text rather than a count with a unit. */
@Composable
private fun TextTile(label: String, value: String, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier.clip(RoundedCornerShape(16.dp)).background(colors.surfaceVariant.copy(alpha = 0.4f)).padding(14.dp),
    ) {
        Eyebrow(label, color = colors.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
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
        // Bars and labels are separate rows: sharing one fixed-height column let a tall bar (a day
        // near the hydration goal) push its own label out of view.
        Row(
            modifier = Modifier.fillMaxWidth().height(barHeight),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            values.forEach { v ->
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight(v / 100f)
                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                        .background(brush),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            labels.forEach { label ->
                Text(
                    label,
                    modifier = Modifier.weight(1f),
                    fontSize = 10.sp,
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(insight, style = MaterialTheme.typography.bodyMedium, color = colors.onSurface.copy(alpha = 0.8f))
    }
}

