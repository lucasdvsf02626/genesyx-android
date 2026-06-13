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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.genesyx.app.domain.content.FocusFood
import com.genesyx.app.domain.model.Phase
import com.genesyx.app.ui.components.Eyebrow
import com.genesyx.app.ui.theme.ElectricLavender
import com.genesyx.app.ui.theme.FoodFollicular
import com.genesyx.app.ui.theme.FoodLuteal
import com.genesyx.app.ui.theme.FoodOvulatory
import com.genesyx.app.ui.theme.FoodPeriod

private fun accentFor(phase: Phase?): Color = when (phase) {
    Phase.PERIOD -> FoodPeriod
    Phase.FOLLICULAR -> FoodFollicular
    Phase.OVULATORY -> FoodOvulatory
    Phase.LUTEAL -> FoodLuteal
    null -> ElectricLavender
}

private data class Supplement(val name: String, val rationale: String)

private val supplements = listOf(
    Supplement("Folate", "Supports egg quality and early neural development."),
    Supplement("Omega-3", "Anti-inflammatory fats for hormone balance."),
    Supplement("Vitamin D", "Linked to healthy ovulation and implantation."),
    Supplement("Zinc", "Aids cell division and progesterone production."),
)

@Composable
fun NutritionScreen(
    navController: NavController,
    viewModel: NutritionViewModel = hiltViewModel(),
) {
    val colors = MaterialTheme.colorScheme
    val state by viewModel.uiState.collectAsState()
    val accent = accentFor(state.phase)
    var expandedFood by remember { mutableStateOf(-1) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(20.dp))

        // ── Header
        Eyebrow(state.phaseHeader, color = ElectricLavender)
        Spacer(Modifier.height(8.dp))
        Text(
            "Your nutrition focus",
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.SemiBold,
            color = colors.onBackground,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            state.headlineSub,
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onSurfaceVariant,
        )

        Spacer(Modifier.height(20.dp))

        // ── Hydration card
        HydrationCard(
            waterMl = state.waterMl,
            goalMl = state.waterGoalMl,
            onAdd = { viewModel.adjustWater(200) },
            onRemove = { viewModel.adjustWater(-200) },
        )

        if (state.cycleSetUp) {
            Spacer(Modifier.height(12.dp))

            // ── Focus foods card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Column(Modifier.padding(vertical = 6.dp)) {
                    state.foods.forEachIndexed { i, food ->
                        FoodRow(
                            food = food,
                            accent = accent,
                            expanded = expandedFood == i,
                            onClick = { expandedFood = if (expandedFood == i) -1 else i },
                        )
                        if (i < state.foods.lastIndex) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp)
                                    .height(1.dp)
                                    .background(colors.outline),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Supplements card
            SupplementsCard()
        }

        Spacer(Modifier.height(24.dp))
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
                        Text(
                            "%.1f".format(waterMl / 1000f),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.onSurface,
                        )
                        Spacer(Modifier.size(4.dp))
                        Text(
                            "/ ${"%.1f".format(goalMl / 1000f)} L",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StepperButton(Icons.Filled.Remove, "Remove 200ml", colors.surfaceVariant, colors.onSurface, onRemove)
                    Spacer(Modifier.size(10.dp))
                    StepperButton(Icons.Filled.Add, "Add 200ml", ElectricLavender, Color.White, onAdd)
                }
            }
            Spacer(Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { (waterMl.toFloat() / goalMl).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
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
private fun StepperButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    cd: String,
    bg: Color,
    fg: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(bg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, cd, tint = fg, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun FoodRow(food: FocusFood, accent: Color, expanded: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(accent),
            )
            Spacer(Modifier.size(12.dp))
            Text(
                food.title,
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurface,
                modifier = Modifier.weight(1f),
            )
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                null,
                tint = colors.onSurfaceVariant,
                modifier = Modifier
                    .size(20.dp)
                    .rotate(if (expanded) 90f else 0f),
            )
        }
        AnimatedVisibility(visible = expanded) {
            Text(
                food.desc,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(start = 22.dp, top = 6.dp),
            )
        }
    }
}

@Composable
private fun SupplementsCard() {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(ElectricLavender.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Medication, null, tint = ElectricLavender, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.size(12.dp))
                Text("Supplements", style = MaterialTheme.typography.titleLarge, color = colors.onSurface)
            }
            Spacer(Modifier.height(14.dp))
            supplements.forEachIndexed { i, s ->
                Column(Modifier.padding(vertical = 6.dp)) {
                    Text(s.name, style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
                    Text(s.rationale, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                }
                if (i < supplements.lastIndex) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(colors.outline.copy(alpha = 0.5f)),
                    )
                }
            }
        }
    }
}
