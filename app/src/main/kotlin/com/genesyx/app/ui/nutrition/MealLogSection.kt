package com.genesyx.app.ui.nutrition

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.genesyx.app.domain.model.MealEntry
import com.genesyx.app.domain.model.MealType
import com.genesyx.app.domain.model.Nutrient
import com.genesyx.app.ui.components.Eyebrow
import com.genesyx.app.ui.components.GxPrimaryButton
import com.genesyx.app.ui.theme.ElectricLavender
import java.time.format.DateTimeFormatter

/**
 * Optional free-text meal note under the food-group chips. Each meal shows its sitting, what she
 * ate, and any nutrient tags. Meals live only on this device (no sync).
 */
@Composable
fun MealLogCard(
    meals: List<MealEntry>,
    onLog: (MealEntry) -> Unit,
    onDelete: (String) -> Unit,
    date: java.time.LocalDate = java.time.LocalDate.now(),
) {
    val colors = MaterialTheme.colorScheme
    // null = closed; a fresh MealEntry with a blank description = the add flow; an existing meal = edit.
    var editing by remember { mutableStateOf<MealEntry?>(null) }
    var confirmDelete by remember { mutableStateOf<MealEntry?>(null) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(20.dp)) {
            Eyebrow("A note about a meal", color = ElectricLavender)
            Spacer(Modifier.height(8.dp))
            if (meals.isEmpty()) {
                Text(
                    "Optional. The chips above are the record; add a description here if you want one. Kept on this device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
            } else {
                meals.forEach { meal ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { editing = meal }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                meal.description,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = colors.onSurface,
                            )
                            val nutrients = meal.nutrients.joinToString(" · ") { it.label }
                            val detail = listOf(
                                meal.type.label,
                                meal.loggedAt.format(TIME_FORMAT),
                            ).joinToString(" · ") + if (nutrients.isNotEmpty()) "  ·  $nutrients" else ""
                            Text(
                                detail,
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { confirmDelete = meal }) {
                            Icon(
                                Icons.Outlined.Delete,
                                "Delete ${meal.description}",
                                tint = colors.onSurfaceVariant,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            GxPrimaryButton(
                text = "Log a meal",
                onClick = { editing = MealEntry(date = date, type = MealType.BREAKFAST, description = "") },
            )
        }
    }

    editing?.let { meal ->
        MealLogDialog(
            initial = meal,
            onDismiss = { editing = null },
            onSave = { onLog(it); editing = null },
        )
    }

    confirmDelete?.let { meal ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            shape = RoundedCornerShape(20.dp),
            containerColor = colors.surface,
            title = { Text("Delete this meal?", style = MaterialTheme.typography.titleLarge, color = colors.onSurface) },
            text = {
                Text(
                    "\"${meal.description}\" will be removed from this day's log.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
            },
            confirmButton = {
                TextButton(onClick = { onDelete(meal.id); confirmDelete = null }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = null }) {
                    Text("Cancel", color = colors.onSurfaceVariant)
                }
            },
        )
    }
}

/** Add or edit one meal: meal type, a free-text description (1..120), and optional nutrient tags.
 *  Editing keeps the meal's id, date and original logged-at time (a plain upsert). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MealLogDialog(
    initial: MealEntry,
    onDismiss: () -> Unit,
    onSave: (MealEntry) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val isNew = initial.description.isBlank()
    var type by remember { mutableStateOf(initial.type) }
    var description by remember { mutableStateOf(initial.description) }
    var nutrients by remember { mutableStateOf(initial.nutrients.toSet()) }
    val valid = description.trim().isNotEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = colors.surface,
        title = {
            Text(
                if (isNew) "Log a meal" else "Edit meal",
                style = MaterialTheme.typography.titleLarge,
                color = colors.onSurface,
            )
        },
        text = {
            Column {
                Eyebrow("Which meal", color = colors.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MealType.entries.forEach { option ->
                        SelectableChip(option.label, type == option) { type = option }
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { if (it.length <= MealEntry.DESCRIPTION_MAX_LENGTH) description = it },
                    label = { Text("What did you eat?") },
                    placeholder = { Text("e.g. Greek yoghurt with berries") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(14.dp))
                Eyebrow("Nutrients (optional)", color = colors.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Nutrient.entries.forEach { nutrient ->
                        val selected = nutrient in nutrients
                        SelectableChip(nutrient.label, selected) {
                            nutrients = if (selected) nutrients - nutrient else nutrients + nutrient
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = {
                    onSave(
                        initial.copy(
                            type = type,
                            description = description.trim(),
                            nutrients = nutrients.toList(),
                        ),
                    )
                },
            ) { Text("Save", color = if (valid) ElectricLavender else colors.onSurfaceVariant) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = colors.onSurfaceVariant) }
        },
    )
}

/** A pill toggle used for both meal type and nutrient tags — same look as the supplement dialog. */
@Composable
private fun SelectableChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Text(
        label,
        style = MaterialTheme.typography.bodyMedium,
        color = if (selected) ElectricLavender else colors.onSurfaceVariant,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) ElectricLavender.copy(alpha = 0.08f) else Color.Transparent)
            .border(1.dp, if (selected) ElectricLavender else colors.outline, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a")
