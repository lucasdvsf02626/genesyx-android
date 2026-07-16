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
import com.genesyx.app.domain.streaks.StreakEngine
import com.genesyx.app.ui.theme.ElectricLavender

/**
 * Sets the goal the whole app measures hydration against. Steps in [StreakEngine.GOAL_STEP_ML] — the
 * same pour the quick-add buttons use — so every reachable goal is one she can land on exactly.
 * [PreferencesRepository][com.genesyx.app.data.PreferencesRepository] clamps on write, so this only
 * ever offers goals inside [StreakEngine.GOAL_RANGE_ML].
 */
@Composable
fun HydrationGoalDialog(current: Int, onDismiss: () -> Unit, onSave: (Int) -> Unit) {
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
                    Text("${"%.1f".format(draft / 1000f)} L", fontSize = 28.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurface)
                    GoalStepper(Icons.Filled.Add, "Raise goal", ElectricLavender, Color.White) {
                        draft = (draft + StreakEngine.GOAL_STEP_ML).coerceIn(StreakEngine.GOAL_RANGE_ML)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(draft) }) { Text("Save", color = ElectricLavender) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = colors.onSurfaceVariant) } },
    )
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
