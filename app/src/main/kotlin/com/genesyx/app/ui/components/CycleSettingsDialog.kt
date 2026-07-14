package com.genesyx.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.genesyx.app.domain.cycle.CycleEngine
import com.genesyx.app.domain.model.CycleSettings
import com.genesyx.app.ui.theme.ElectricLavender
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val dateLabelFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy")

private fun LocalDate.toUtcMillis(): Long =
    atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.utcMillisToLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

/**
 * "Your cycle" dialog — last period date + cycle/period length. Mirrors the web
 * CycleSettingsDialog (docs/SCREEN_LAYOUTS.md). Validation matches the data layer:
 * cycle length 21–35, period length 1–10.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CycleSettingsDialog(
    current: CycleSettings?,
    onDismiss: () -> Unit,
    onSave: (CycleSettings) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val today = remember { LocalDate.now() }

    // Null until the user actually picks one. It used to default to `today`, so opening the dialog
    // and tapping Save silently committed "my period started today" — a date she never entered —
    // and Home then rendered "DAY 1 · PERIOD" from it with complete confidence. Every phase,
    // fertile window and prediction in the app is derived from this one value, so it must come
    // from her or not exist.
    var lastPeriod by remember { mutableStateOf(current?.lastPeriodDate) }
    var cycleLength by remember { mutableStateOf(current?.cycleLength ?: CycleEngine.DEFAULT_CYCLE_LENGTH) }
    var periodLength by remember { mutableStateOf(current?.periodLength ?: CycleEngine.DEFAULT_PERIOD_LENGTH) }
    var showDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = colors.surface,
        title = { Text("Your cycle", style = MaterialTheme.typography.titleLarge, color = colors.onSurface) },
        text = {
            Column {
                Text(
                    "We use this to predict your phases and fertile window.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))

                Eyebrow("First day of last period", color = colors.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, colors.outline, RoundedCornerShape(12.dp))
                        .background(colors.surface)
                        .clickable { showDatePicker = true }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(
                        lastPeriod?.format(dateLabelFormat) ?: "Select date",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (lastPeriod != null) colors.onSurface else colors.onSurfaceVariant,
                    )
                }

                Spacer(Modifier.height(16.dp))
                NumberStepper(
                    label = "Cycle length",
                    value = cycleLength,
                    suffix = "days",
                    onDecrement = { if (cycleLength > CycleEngine.CYCLE_LENGTH_RANGE.first) cycleLength-- },
                    onIncrement = { if (cycleLength < CycleEngine.CYCLE_LENGTH_RANGE.last) cycleLength++ },
                )

                Spacer(Modifier.height(12.dp))
                NumberStepper(
                    label = "Period length",
                    value = periodLength,
                    suffix = "days",
                    onDecrement = { if (periodLength > CycleEngine.PERIOD_LENGTH_RANGE.first) periodLength-- },
                    onIncrement = { if (periodLength < CycleEngine.PERIOD_LENGTH_RANGE.last) periodLength++ },
                )
            }
        },
        confirmButton = {
            // No date, no save — there is nothing to derive a cycle from.
            val chosen = lastPeriod
            TextButton(
                enabled = chosen != null,
                onClick = { if (chosen != null) onSave(CycleSettings(chosen, cycleLength, periodLength)) },
            ) {
                Text(
                    "Save",
                    color = if (chosen != null) ElectricLavender else colors.onSurfaceVariant.copy(alpha = 0.5f),
                    fontWeight = FontWeight.SemiBold,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = colors.onSurfaceVariant)
            }
        },
    )

    if (showDatePicker) {
        val state = rememberDatePickerState(
            // Nothing pre-selected when she has never set one: pre-selecting today would let two
            // taps recreate the very default this fix removes.
            initialSelectedDateMillis = lastPeriod?.toUtcMillis(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    utcTimeMillis <= today.toUtcMillis()
            },
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                val picked = state.selectedDateMillis
                TextButton(
                    enabled = picked != null,
                    onClick = {
                        picked?.let { lastPeriod = it.utcMillisToLocalDate() }
                        showDatePicker = false
                    },
                ) {
                    Text(
                        "OK",
                        color = if (picked != null) ElectricLavender else colors.onSurfaceVariant.copy(alpha = 0.5f),
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = colors.onSurfaceVariant)
                }
            },
        ) {
            DatePicker(state = state)
        }
    }
}

/** Label + round −/+ stepper used by the cycle settings dialog. */
@Composable
private fun NumberStepper(
    label: String,
    value: Int,
    suffix: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(label, style = MaterialTheme.typography.bodyLarge, color = colors.onSurface)
            Text("$value $suffix", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            StepperButton(Icons.Filled.Remove, "Decrease $label", onDecrement)
            Spacer(Modifier.size(8.dp))
            StepperButton(Icons.Filled.Add, "Increase $label", onIncrement)
        }
    }
}

@Composable
private fun StepperButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(colors.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        IconButton(onClick = onClick, modifier = Modifier.size(36.dp)) {
            Icon(icon, contentDescription = contentDescription, tint = colors.onSurface, modifier = Modifier.size(18.dp))
        }
    }
}
