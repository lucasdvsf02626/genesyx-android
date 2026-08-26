package com.genesyx.app.ui.nutrition

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.genesyx.app.domain.content.PhaseFood
import com.genesyx.app.domain.hydration.HydrationCoach
import com.genesyx.app.domain.hydration.HydrationFormat
import com.genesyx.app.domain.hydration.HydrationUnit
import com.genesyx.app.domain.content.AppGuide
import com.genesyx.app.domain.content.ArticleCategory
import com.genesyx.app.domain.content.LearnDrip
import com.genesyx.app.domain.content.recipesFor
import com.genesyx.app.ui.components.ExpandableInfo
import com.genesyx.app.domain.model.SupplementPlanEntry
import com.genesyx.app.domain.model.SupplementToggleSet
import com.genesyx.app.ui.components.ConsentWithdrawnBanner
import com.genesyx.app.ui.components.Eyebrow
import com.genesyx.app.ui.components.GxPrimaryButton
import com.genesyx.app.ui.components.HydrationChallengeCard
import com.genesyx.app.ui.components.HydrationGoalDialog
import com.genesyx.app.ui.learn.HowThisWorksLink
import com.genesyx.app.ui.navigation.Screen
import com.genesyx.app.ui.navigation.navigateToTab
import com.genesyx.app.ui.theme.ElectricLavender
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Composable
fun NutritionScreen(
    navController: NavController,
    /** Arrive with the supplement plan already open (pH's "See your supplement plan" card). */
    openPlan: Boolean = false,
    /** Bumped when the Nutrition tab is tapped while already selected — scroll back to the top. */
    reselects: StateFlow<Int> = MutableStateFlow(0),
    viewModel: NutritionViewModel = hiltViewModel(),
) {
    val colors = MaterialTheme.colorScheme
    val state by viewModel.uiState.collectAsState()
    val userSupplements by viewModel.userSupplements.collectAsState()
    val planEntries by viewModel.planEntries.collectAsState()
    val planReminders by viewModel.planReminders.collectAsState()
    val supplementReminders by viewModel.supplementReminders.collectAsState()
    val todaysMeals by viewModel.todaysMeals.collectAsState()
    val catalogue by viewModel.catalogue.collectAsState()
    val glassMl by viewModel.glassMl.collectAsState()
    val consentActive by viewModel.consentActive.collectAsState()
    var expandedFood by remember { mutableStateOf<String?>(null) }
    var expandedRecipe by remember { mutableStateOf<String?>(null) }
    // Saveable so a rotation after she dismisses it doesn't re-open the plan from `openPlan`.
    var planOpen by rememberSaveable { mutableStateOf(openPlan) }
    var goalOpen by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val snackbar = remember { SnackbarHostState() }

    // Re-tapping the selected tab scrolls to the top rather than pushing a second Nutrition.
    val reselect by reselects.collectAsState()
    var seenReselect by rememberSaveable { mutableIntStateOf(reselect) }
    LaunchedEffect(reselect) {
        if (reselect != seenReselect) {
            seenReselect = reselect
            scrollState.animateScrollTo(0)
        }
    }

    // A chip tap that did not simply save says so — refused, queued offline, or failed (Retry).
    LaunchedEffect(Unit) {
        viewModel.supplementEvents.collect { event ->
            val result = snackbar.showSnackbar(
                message = event.text,
                actionLabel = if (event is SupplementSaveEvent.Failed) "Retry" else null,
                withDismissAction = true,
                duration = if (event is SupplementSaveEvent.Queued) SnackbarDuration.Short else SnackbarDuration.Long,
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.toggleSupplement(event.entry)
        }
    }

    com.genesyx.app.ui.components.GenesyxPage {
    Box(Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
    ) {
        // ── Header (px-6)
        Column(Modifier.padding(horizontal = 24.dp).padding(top = 20.dp, bottom = 12.dp)) {
            Eyebrow(state.phaseHeader, color = ElectricLavender)
            Spacer(Modifier.height(8.dp))
            Text(
                "Your nutrition focus",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.SemiBold,
                color = colors.onBackground,
            )
            Spacer(Modifier.height(10.dp))
            Text(state.headlineSub, style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
        }

        Column(Modifier.padding(horizontal = 20.dp)) {
            if (!consentActive) {
                ConsentWithdrawnBanner()
                Spacer(Modifier.height(12.dp))
            }

            // ── The plan, first: tap a chip to log it for today (iOS order). Not gated on the
            // cycle being set up — taking a supplement has nothing to do with the calendar.
            SupplementPlanCard(
                entries = planEntries,
                loggedToday = state.loggedToday,
                onToggle = { viewModel.toggleSupplement(it) },
                onReview = { planOpen = true },
            )

            // ── Hydration card
            Spacer(Modifier.height(12.dp))
            HydrationCard(
                waterMl = state.waterMl,
                goalMl = state.waterGoalMl,
                unit = state.waterUnit,
                glassMl = glassMl,
                coaching = state.hydrationCoaching,
                weeklyStreak = state.weeklyStreak,
                daysOnGoal = state.daysOnGoal,
                onAdd = { viewModel.adjustWater(glassMl) },
                onRemove = { viewModel.adjustWater(-glassMl) },
                onEditGoal = { goalOpen = true },
                onTrack = { navController.navigate(Screen.HydrationDetail.route) },
            )

            Spacer(Modifier.height(12.dp))
            HydrationChallengeCard(
                days = state.hydrationChallengeDays,
                onOpen = { navController.navigate(Screen.HydrationDetail.route) },
            )

            // Action-first ordering: the things she DOES (log water, keep her supplement list)
            // come before the things she READS (focus foods, articles).
            // pH moved out to its own bottom tab (client request, 12 Aug 2026).
            Spacer(Modifier.height(12.dp))
            UserSupplementsCard(
                supplements = userSupplements,
                reminders = supplementReminders,
                onSave = { viewModel.saveSupplement(it) },
                onDelete = { viewModel.deleteSupplement(it.id) },
                onSetReminder = { id, name, minutes -> viewModel.setSupplementReminder(id, name, minutes) },
            )

            Spacer(Modifier.height(12.dp))
            FoodGroupCard(
                logged = state.foodGroups,
                phase = state.phase,
                onToggle = { viewModel.toggleFoodGroup(it) },
            )

            Spacer(Modifier.height(12.dp))
            MealLogCard(
                meals = todaysMeals,
                date = java.time.LocalDate.now(),
                onLog = { viewModel.logMeal(it) },
                onDelete = { viewModel.deleteMeal(it) },
            )

            Spacer(Modifier.height(12.dp))
            GenesyxRangeCard(
                products = catalogue,
                addedProductIds = userSupplements.mapNotNull { it.productId }.toSet(),
                onAdd = { viewModel.addFromCatalogue(it) },
            )

            if (state.cycleSetUp) {
                Spacer(Modifier.height(12.dp))
                FocusFoodsCard(state.foods, expandedFood) { name ->
                    expandedFood = if (expandedFood == name) null else name
                }
            }

            // Recipes: phase-relevant plus always-shown general ones (empty → "coming soon"). Outside
            // the cycle gate so the section — and its coming-soon note — is visible from day one.
            Spacer(Modifier.height(16.dp))
            RecipesSection(
                recipes = recipesFor(state.phase),
                expandedId = expandedRecipe,
                onToggle = { expandedRecipe = if (expandedRecipe == it) null else it },
                onLogGroups = { viewModel.logFoodGroups(it.toSet()) },
            )

            // Outside the cycle gate: Learn is most useful to someone who hasn't set up a cycle yet.
            // "See all articles" targets another TAB, so it must switch tabs the way the bottom
            // bar does — a plain push put Learn on top of Nutrition and killed the tab (SFM-27).
            Spacer(Modifier.height(16.dp))
            ArticlesSection(
                onOpen = { navController.navigate(Screen.ArticleDetail.create(it)) },
                onSeeAll = { navController.navigateToTab(Screen.Learn) },
            )

            HowThisWorksLink(
                slug = AppGuide.NUTRITION,
                label = AppGuide.NUTRITION_LABEL,
                onOpen = { navController.navigate(Screen.ArticleDetail.create(it)) },
            )

            Spacer(Modifier.height(24.dp))
        }
    }
    SnackbarHost(hostState = snackbar, modifier = Modifier.align(Alignment.BottomCenter))
    }
    }

    if (goalOpen) {
        // The shared dialog — one goal editor (and one ml/cups + glass-size toggle) app-wide.
        HydrationGoalDialog(
            current = state.waterGoalMl,
            unit = state.waterUnit,
            glassMl = glassMl,
            onUnitChange = { viewModel.setWaterUnit(it) },
            onGlassChange = { viewModel.setGlassMl(it) },
            onDismiss = { goalOpen = false },
            onSave = { viewModel.setWaterGoal(it); goalOpen = false },
        )
    }

    if (planOpen) {
        SupplementPlanSheet(
            customSupplements = userSupplements,
            planReminders = planReminders,
            customReminders = supplementReminders,
            onSetPlanReminder = { supplement, minutes -> viewModel.setPlanReminder(supplement, minutes) },
            onSetCustomReminder = { entry, minutes -> viewModel.setSupplementReminder(entry.id, entry.name, minutes) },
            onAddSupplement = { viewModel.saveSupplement(it) },
            onDismiss = { planOpen = false },
        )
    }
}

@Composable
private fun HydrationCard(
    waterMl: Int,
    goalMl: Int,
    unit: HydrationUnit,
    glassMl: Int,
    coaching: String,
    weeklyStreak: Int,
    daysOnGoal: Int,
    onAdd: () -> Unit,
    onRemove: () -> Unit,
    onEditGoal: () -> Unit,
    onTrack: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val remaining = (goalMl - waterMl).coerceAtLeast(0)
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                    Eyebrow("Hydration", color = colors.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(HydrationFormat.format(waterMl, unit), fontSize = 28.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurface)
                        Spacer(Modifier.size(4.dp))
                        Text("/ ${HydrationFormat.format(goalMl, unit)} goal", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, modifier = Modifier.padding(bottom = 4.dp))
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StepperButton(Icons.Filled.Remove, "Remove a glass (${glassMl}ml)", colors.surfaceVariant, colors.onSurface, onRemove)
                    Spacer(Modifier.size(8.dp))
                    StepperButton(Icons.Filled.Add, "Add a glass (${glassMl}ml)", ElectricLavender, Color.White, onAdd)
                }
            }
            Spacer(Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { (waterMl.toFloat() / goalMl).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                color = colors.onSurface,
                trackColor = colors.surfaceVariant,
                // M3 draws a stop dot at the track's end by default — on an empty bar it floats
                // alone at the far right and reads as a stray goal marker.
                drawStopIndicator = {},
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.WaterDrop, null, tint = colors.onSurfaceVariant, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.size(6.dp))
                    Text(
                        if (remaining > 0) "${HydrationFormat.format(remaining, unit)} to go" else "Goal reached — nice work",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                    )
                }
                // TextButtons, not tappable labels: they carry Material's 48dp touch target.
                Row {
                    TextButton(onClick = onEditGoal) {
                        Text("Edit goal", style = MaterialTheme.typography.bodyMedium, color = ElectricLavender)
                    }
                    TextButton(onClick = onTrack) {
                        Text("Track ›", style = MaterialTheme.typography.bodyMedium, color = ElectricLavender)
                    }
                }
            }
            // Intraday pacing — framed by the time of day, a nudge rather than a verdict.
            if (coaching.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(coaching, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant.copy(alpha = 0.8f))
            }
            // One compact stats line, not a paragraph of faded text — the card is for acting on.
            val stats = buildList {
                if (daysOnGoal == 1) add("1 day on goal this week")
                if (daysOnGoal > 1) add("$daysOnGoal days on goal this week")
                if (weeklyStreak == 1) add("1 steady week")
                if (weeklyStreak > 1) add("$weeklyStreak steady weeks")
            }
            if (stats.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    stats.joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant.copy(alpha = 0.7f),
                )
            }
            Spacer(Modifier.height(8.dp))
            ExpandableInfo(label = HydrationCoach.WHY_TITLE, body = HydrationCoach.WHY_TEXT)
            Spacer(Modifier.height(10.dp))
            com.genesyx.app.ui.components.CitationList(
                title = "Sources",
                citations = listOf(
                    com.genesyx.app.domain.content.MedicalSources.require("armstrong-2012"),
                    com.genesyx.app.domain.content.MedicalSources.require("valtin-2002"),
                    com.genesyx.app.domain.content.MedicalSources.require("nhs-water"),
                ),
            )
        }
    }
}

@Composable
private fun StepperButton(icon: androidx.compose.ui.graphics.vector.ImageVector, cd: String, bg: Color, fg: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(36.dp).clip(CircleShape).background(bg).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, cd, tint = fg, modifier = Modifier.size(18.dp))
    }
}

/**
 * Focus foods as individual, attractive cards (an accent header strip + a "Why this helps"
 * expandable) rather than one flat text list — the client's "replace text-only food suggestions
 * with meal/recipe cards". Content is unchanged ([PhaseFood]); this is presentation only.
 */
@Composable
private fun FocusFoodsCard(foods: List<PhaseFood>, expanded: String?, onToggle: (String) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column {
        Eyebrow("Focus foods", color = colors.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
        Text(
            "Your focus foods this phase",
            style = MaterialTheme.typography.titleLarge,
            color = colors.onSurface,
            modifier = Modifier.padding(start = 4.dp, bottom = 12.dp),
        )
        foods.forEach { food ->
            val open = expanded == food.name
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .clickable { onToggle(food.name) },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Column {
                    // Accent header band — gives each food its own colour identity.
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .background(Brush.horizontalGradient(listOf(food.accent, food.accent.copy(alpha = 0.45f)))),
                    )
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(food.accent.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center,
                        ) { Box(Modifier.size(14.dp).clip(CircleShape).background(food.accent)) }
                        Spacer(Modifier.size(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(food.name, style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
                            Spacer(Modifier.height(2.dp))
                            Text(food.shortDesc, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                        }
                        Icon(
                            Icons.Filled.ChevronRight,
                            if (open) "Hide details" else "Show details",
                            tint = colors.onSurfaceVariant,
                            modifier = Modifier.size(18.dp).rotate(if (open) 90f else 0f),
                        )
                    }
                    AnimatedVisibility(visible = open) {
                        Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                            Text(
                                "Why this helps",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = food.accent,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(food.expandedDesc, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

/**
 * "Your supplement plan" — the four bundled essentials plus her own entries as tappable chips.
 * A tap logs or un-logs that supplement for today through the daily-log repository; the chip
 * fills from the row Room emits, so it reflects what is actually stored, never a guess.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SupplementPlanCard(
    entries: List<SupplementPlanEntry>,
    loggedToday: Set<String>,
    onToggle: (SupplementPlanEntry) -> Unit,
    onReview: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val taken = SupplementToggleSet.takenCount(entries, loggedToday)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row {
                Box(
                    Modifier.size(44.dp).clip(RoundedCornerShape(16.dp)).background(ElectricLavender.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Filled.Medication, null, tint = ElectricLavender) }
                Spacer(Modifier.size(16.dp))
                Column(Modifier.weight(1f)) {
                    Text("Your supplement plan", style = MaterialTheme.typography.titleLarge, color = colors.onSurface)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Folate, Omega-3, Vitamin D, and Zinc — taken with breakfast.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                entries.forEachIndexed { i, entry ->
                    PlanChip(
                        entry = entry,
                        index = i,
                        logged = SupplementToggleSet.isLogged(entry, loggedToday),
                        onToggle = { onToggle(entry) },
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            // Live from today's row — iOS's plan-card line, same strings.
            Text(
                SupplementToggleSet.statusLine(taken, entries.size),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (taken == 0) colors.onSurfaceVariant else ElectricLavender,
            )
            Spacer(Modifier.height(12.dp))
            // Secondary rationale tucked behind a tap — keeps the card action-focused (client's
            // "Why is this important? / Learn more" dropdown).
            ExpandableInfo(
                label = "Why is this important?",
                body = "Folate lowers the risk of neural-tube conditions and matters most in the " +
                    "first weeks — before many people know they're pregnant, so it's worth taking " +
                    "while trying. Vitamin D is advised for UK adults through autumn and winter. " +
                    "Omega-3 and zinc support general health. This is a suggested starting point, " +
                    "not a prescription — a pharmacist or GP can tailor it to you.",
            )
            Spacer(Modifier.height(12.dp))
            GxPrimaryButton(text = "Review Plan", onClick = onReview)
        }
    }
}

/** One tappable chip: the initial in a ring, filled solid once logged. 44dp — a real touch target. */
@Composable
private fun PlanChip(entry: SupplementPlanEntry, index: Int, logged: Boolean, onToggle: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val tint = planTint(index)
    val label = "${entry.display}, ${if (logged) "logged" else "not logged"} today"
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(64.dp)
            .toggleable(value = logged, role = Role.Checkbox, onValueChange = { onToggle() })
            .semantics { contentDescription = label },
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(if (logged) tint else tint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                entry.initial,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (logged) Color.White else tint,
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            entry.display,
            style = MaterialTheme.typography.labelSmall,
            color = if (logged) colors.onSurface else colors.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Entry point into the Learn section. Each tile opens its own article; "See all" opens the landing. */
@Composable
private fun ArticlesSection(
    onOpen: (String) -> Unit,
    onSeeAll: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Column {
        Eyebrow("Learn more", color = colors.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp, bottom = 10.dp))
        // A taster, not the library — showing every row made the tab scroll forever. Learn is one
        // tap away. Same LearnDrip gate (published-by-date) as every other article surface.
        LearnDrip.published(java.time.LocalDate.now())
            .filter { it.category == ArticleCategory.NUTRITION }
            .take(3)
            .forEach { a ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.surface)
                    .clickable { onOpen(a.slug) }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(a.title, style = MaterialTheme.typography.labelLarge, color = colors.onSurface)
                    Spacer(Modifier.height(2.dp))
                    Text(a.readingTime, fontSize = 11.5.sp, color = colors.onSurfaceVariant)
                }
                Icon(Icons.Filled.ChevronRight, null, tint = colors.onSurfaceVariant, modifier = Modifier.size(18.dp))
            }
        }
        TextButton(onClick = onSeeAll, modifier = Modifier.padding(start = 4.dp)) {
            Text("See all articles", style = MaterialTheme.typography.bodyMedium, color = ElectricLavender)
        }
    }
}
