package com.genesyx.app.ui.nutrition

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.genesyx.app.domain.content.PhaseFood
import com.genesyx.app.domain.content.learnArticles
import com.genesyx.app.domain.content.supplementPlan
import com.genesyx.app.ui.components.Eyebrow
import com.genesyx.app.ui.components.GxPrimaryButton
import com.genesyx.app.ui.navigation.Screen
import com.genesyx.app.ui.ph.PhTrackerSection
import com.genesyx.app.ui.theme.ElectricBlue
import com.genesyx.app.ui.theme.ElectricLavender
import com.genesyx.app.ui.theme.ElectricPink

@Composable
fun NutritionScreen(
    navController: NavController,
    viewModel: NutritionViewModel = hiltViewModel(),
) {
    val colors = MaterialTheme.colorScheme
    val state by viewModel.uiState.collectAsState()
    var expandedFood by remember { mutableStateOf<String?>(null) }
    var planOpen by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState()),
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
            // ── Hydration card
            HydrationCard(
                waterMl = state.waterMl,
                goalMl = state.waterGoalMl,
                onAdd = { viewModel.adjustWater(200) },
                onRemove = { viewModel.adjustWater(-200) },
            )

            if (com.genesyx.app.core.FeatureFlags.PH_TRACKING) {
                Spacer(Modifier.height(12.dp))
                PhTrackerSection()
            }

            if (state.cycleSetUp) {
                Spacer(Modifier.height(12.dp))
                FocusFoodsCard(state.foods, expandedFood) { name ->
                    expandedFood = if (expandedFood == name) null else name
                }

                Spacer(Modifier.height(12.dp))
                SupplementPlanCard(onReview = { planOpen = true })
            }

            // Outside the cycle gate: Learn is most useful to someone who hasn't set up a cycle yet.
            Spacer(Modifier.height(16.dp))
            ArticlesSection(
                onOpen = { navController.navigate(Screen.ArticleDetail.create(it)) },
                onSeeAll = { navController.navigate(Screen.Learn.route) },
            )

            Spacer(Modifier.height(24.dp))
        }
    }

    if (planOpen) {
        AlertDialog(
            onDismissRequest = { planOpen = false },
            shape = RoundedCornerShape(20.dp),
            containerColor = colors.surface,
            title = { Text("Your supplement plan", style = MaterialTheme.typography.titleLarge, color = colors.onSurface) },
            text = {
                Column {
                    Text(
                        "Gentle, evidence-informed essentials for fertility prep.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    supplementPlan.forEachIndexed { i, s ->
                        Row(Modifier.padding(vertical = 6.dp)) {
                            SupplementAvatar(s.initial, i)
                            Spacer(Modifier.size(12.dp))
                            Column {
                                Text(s.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = colors.onSurface)
                                Text(s.rationale, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { planOpen = false }) { Text("Got it", color = ElectricLavender) } },
        )
    }
}

@Composable
private fun HydrationCard(waterMl: Int, goalMl: Int, onAdd: () -> Unit, onRemove: () -> Unit) {
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
                        Text("%.1f".format(waterMl / 1000f), fontSize = 28.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurface)
                        Spacer(Modifier.size(4.dp))
                        Text("/ ${"%.1f".format(goalMl / 1000f)} L", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, modifier = Modifier.padding(bottom = 4.dp))
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StepperButton(Icons.Filled.Remove, "Remove 200ml", colors.surfaceVariant, colors.onSurface, onRemove)
                    Spacer(Modifier.size(8.dp))
                    StepperButton(Icons.Filled.Add, "Add 200ml", ElectricLavender, Color.White, onAdd)
                }
            }
            Spacer(Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { (waterMl.toFloat() / goalMl).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                color = colors.onSurface,
                trackColor = colors.surfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.WaterDrop, null, tint = colors.onSurfaceVariant, modifier = Modifier.size(14.dp))
                Spacer(Modifier.size(6.dp))
                Text(
                    if (remaining > 0) "${remaining}ml to go" else "Target reached — nice work",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
            }
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

@Composable
private fun FocusFoodsCard(foods: List<PhaseFood>, expanded: String?, onToggle: (String) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column {
            Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 12.dp)) {
                Eyebrow("Focus foods", color = colors.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Text("Your focus foods this phase", style = MaterialTheme.typography.titleLarge, color = colors.onSurface)
            }
            foods.forEachIndexed { i, food ->
                val open = expanded == food.name
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggle(food.name) }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                ) {
                    Box(Modifier.padding(top = 6.dp).size(12.dp).clip(CircleShape).background(food.accent))
                    Spacer(Modifier.size(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(food.name, style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
                        Spacer(Modifier.height(2.dp))
                        Text(food.shortDesc, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                        AnimatedVisibility(visible = open) {
                            Text(
                                food.expandedDesc,
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.onSurfaceVariant.copy(alpha = 0.8f),
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                    }
                    Icon(
                        Icons.Filled.ChevronRight,
                        null,
                        tint = colors.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp).size(18.dp).rotate(if (open) 90f else 0f),
                    )
                }
                if (i < foods.lastIndex) {
                    Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(1.dp).background(colors.outline.copy(alpha = 0.6f)))
                }
            }
        }
    }
}

@Composable
private fun SupplementPlanCard(onReview: () -> Unit) {
    val colors = MaterialTheme.colorScheme
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
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        supplementPlan.forEachIndexed { i, s ->
                            Box(Modifier.offset(x = (i * -6).dp)) { SupplementAvatar(s.initial, i, bordered = true) }
                        }
                        Spacer(Modifier.size(8.dp))
                        Text("3 of 4 taken today", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            GxPrimaryButton(text = "Review Plan", onClick = onReview)
        }
    }
}

@Composable
private fun SupplementAvatar(initial: String, index: Int, bordered: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    val tint = when (index) {
        1 -> ElectricBlue
        3 -> ElectricPink
        else -> ElectricLavender
    }
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(initial, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = tint)
    }
}

/** Entry point into the Learn section. Each tile opens its own article; "See all" opens the landing. */
@Composable
private fun ArticlesSection(onOpen: (String) -> Unit, onSeeAll: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column {
        Eyebrow("Learn more", color = colors.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp, bottom = 10.dp))
        learnArticles.forEach { a ->
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
