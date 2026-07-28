package com.genesyx.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.genesyx.app.domain.hydration.HydrationFormat
import com.genesyx.app.domain.hydration.HydrationUnit
import com.genesyx.app.domain.streaks.StreakEngine
import com.genesyx.app.ui.theme.ElectricLavender

/**
 * Sets the goal the whole app measures hydration against. Steps in [StreakEngine.GOAL_STEP_ML] — the
 * same pour the quick-add buttons use — so every reachable goal is one she can land on exactly.
 * [PreferencesRepository][com.genesyx.app.data.PreferencesRepository] clamps on write, so this only
 * ever offers goals inside [StreakEngine.GOAL_RANGE_ML].
 *
 * Also hosts the ml/cups display choice. Applied immediately (it's a display preference, not part
 * of the goal draft) and it changes only how amounts are *shown* — storage stays in ml.
 */
@Composable
fun HydrationGoalDialog(
    current: Int,
    unit: HydrationUnit,
    onUnitChange: (HydrationUnit) -> Unit,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    var draft by remember(current) { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = colors.surface,
        title = { Text("Daily water goal", style = MaterialTheme.typography.titleLarge, color = colors.onSurface) },
        text = {
            Column {
                Text(
                    "How much you're aiming for each day. Your streaks are measured against this.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GoalStepper(Icons.Filled.Remove, "Lower goal", colors.surfaceVariant, colors.onSurface) {
                        draft = (draft - StreakEngine.GOAL_STEP_ML).coerceIn(StreakEngine.GOAL_RANGE_ML)
                    }
                    Text(HydrationFormat.format(draft, unit), fontSize = 28.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurface)
                    GoalStepper(Icons.Filled.Add, "Raise goal", ElectricLavender, Color.White) {
                        draft = (draft + StreakEngine.GOAL_STEP_ML).coerceIn(StreakEngine.GOAL_RANGE_ML)
                    }
                }
                Spacer(Modifier.height(20.dp))
                Eyebrow("Show amounts as", color = colors.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(colors.surfaceVariant).padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    UnitChip("ml / L", unit == HydrationUnit.ML, Modifier.weight(1f)) { onUnitChange(HydrationUnit.ML) }
                    UnitChip("Cups (250ml)", unit == HydrationUnit.CUPS, Modifier.weight(1f)) { onUnitChange(HydrationUnit.CUPS) }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(draft) }) { Text("Save", color = ElectricLavender) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = colors.onSurfaceVariant) } },
    )
}

@Composable
private fun UnitChip(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) colors.surface else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = if (selected) colors.onSurface else colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun GoalStepper(icon: ImageVector, cd: String, bg: Color, fg: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(44.dp).clip(CircleShape).background(bg).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, cd, tint = fg, modifier = Modifier.size(20.dp))
    }
}
