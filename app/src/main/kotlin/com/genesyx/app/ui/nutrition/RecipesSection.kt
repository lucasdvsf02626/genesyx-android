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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.genesyx.app.domain.content.Recipe
import com.genesyx.app.domain.model.Phase
import com.genesyx.app.ui.components.Eyebrow
import com.genesyx.app.ui.theme.ElectricLavender

/**
 * Recipe cards for the Nutrition tab. Each recipe is an expandable card (accent header band, title,
 * one-line hook, prep time + nutrient chips) that opens to its ingredients and steps. While there is
 * no content yet it shows a "coming soon" state — the same graceful-empty pattern as the Genesyx
 * range catalogue.
 */
@Composable
fun RecipesSection(
    recipes: List<Recipe>,
    expandedId: String?,
    onToggle: (String) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Column {
        Eyebrow("Recipes", color = colors.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
        Text(
            "Recipes for your cycle",
            style = MaterialTheme.typography.titleLarge,
            color = colors.onSurface,
            modifier = Modifier.padding(start = 4.dp, bottom = 12.dp),
        )
        if (recipes.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Text(
                    "Cycle-friendly recipes are coming soon. They'll appear here the moment they're added.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(20.dp),
                )
            }
        } else {
            recipes.forEach { recipe ->
                RecipeCard(recipe, open = expandedId == recipe.id, onClick = { onToggle(recipe.id) })
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecipeCard(recipe: Recipe, open: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val accent = accentFor(recipe.phase)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(Brush.horizontalGradient(listOf(accent, accent.copy(alpha = 0.45f)))),
            )
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(recipe.title, style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
                    Spacer(Modifier.height(2.dp))
                    Text(recipe.subtitle, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                    val meta = buildList {
                        recipe.prepMinutes?.let { add("${it} min") }
                        recipe.phase?.let { add(phaseWord(it)) }
                    }
                    if (meta.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            meta.joinToString(" · "),
                            style = MaterialTheme.typography.labelMedium,
                            color = accent,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                Icon(
                    Icons.Filled.ChevronRight,
                    if (open) "Hide recipe" else "Show recipe",
                    tint = colors.onSurfaceVariant,
                    modifier = Modifier.size(18.dp).rotate(if (open) 90f else 0f),
                )
            }
            AnimatedVisibility(visible = open) {
                Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                    if (recipe.nutrients.isNotEmpty()) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            recipe.nutrients.forEach { NutrientChip(it.label, accent) }
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                    if (recipe.ingredients.isNotEmpty()) {
                        Label("Ingredients", accent)
                        Spacer(Modifier.height(4.dp))
                        recipe.ingredients.forEach { line ->
                            Text("· $line", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                    if (recipe.steps.isNotEmpty()) {
                        Label("Method", accent)
                        Spacer(Modifier.height(4.dp))
                        recipe.steps.forEachIndexed { i, step ->
                            Text(
                                "${i + 1}. $step",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 2.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Label(text: String, accent: Color) {
    Text(text, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = accent)
}

@Composable
private fun NutrientChip(label: String, accent: Color) {
    Text(
        label,
        style = MaterialTheme.typography.labelMedium,
        color = accent,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(accent.copy(alpha = 0.10f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

/** Phase-tinted accent so a recipe card sits visually with its phase's focus foods; general
 *  (any-time) recipes use the app accent. */
private fun accentFor(phase: Phase?): Color = when (phase) {
    Phase.PERIOD -> Color(0xFFF48FB1)
    Phase.FOLLICULAR -> Color(0xFFA5D6A7)
    Phase.OVULATORY -> Color(0xFFCE93D8)
    Phase.LUTEAL -> Color(0xFFB39DDB)
    null -> ElectricLavender
}

private fun phaseWord(phase: Phase): String = when (phase) {
    Phase.PERIOD -> "Period"
    Phase.FOLLICULAR -> "Follicular"
    Phase.OVULATORY -> "Ovulatory"
    Phase.LUTEAL -> "Luteal"
}
