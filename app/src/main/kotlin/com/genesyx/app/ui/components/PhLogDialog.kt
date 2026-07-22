package com.genesyx.app.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
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
import androidx.compose.ui.unit.sp
import com.genesyx.app.domain.model.PhReading
import com.genesyx.app.domain.ph.PhCopy
import com.genesyx.app.domain.ph.PhStatus
import com.genesyx.app.ui.theme.ElectricLavender
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private val whenFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy · HH:mm")

/**
 * Log / edit a vaginal-pH reading: big value tile coloured by status, slider 3.5–7.0 step 0.1
 * (provisional range — see PhStatus) with ± buttons, when picker, notes, Save (+ Delete when
 * editing). Editing preserves the reading's measurement type (a legacy urine reading stays urine).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhLogDialog(
    existing: PhReading?,
    onDismiss: () -> Unit,
    onSave: (PhReading) -> Unit,
    onDelete: ((String) -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme

    var value by remember { mutableStateOf(existing?.phValue ?: 4.2) }
    var recordedAt by remember { mutableStateOf(existing?.recordedAt ?: LocalDateTime.now()) }
    var notes by remember { mutableStateOf(existing?.notes.orEmpty()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val status = PhStatus.classify(value)

    fun clampRound(v: Double) = (((v * 10).roundToInt()) / 10.0).coerceIn(PhStatus.MIN, PhStatus.MAX)

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = colors.surface,
        title = {
            Text(
                if (existing == null) "Log pH reading" else "Edit pH reading",
                style = MaterialTheme.typography.titleLarge,
                color = colors.onSurface,
            )
        },
        text = {
            Column {
                Text(
                    "Track your vaginal pH from 3.5 to 7.0.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))

                // Big value tile
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(status.color.copy(alpha = 0.10f))
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "%.1f".format(value),
                            fontSize = 44.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = status.color,
                        )
                        Spacer(Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(status.color.copy(alpha = 0.18f))
                                .padding(horizontal = 10.dp, vertical = 3.dp),
                        ) {
                            Text(
                                status.label.uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = status.color,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Slider row with ± buttons
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RoundIconButton(Icons.Filled.Remove, "Decrease pH") {
                        value = clampRound(value - PhStatus.STEP)
                    }
                    Slider(
                        value = value.toFloat(),
                        onValueChange = { value = clampRound(it.toDouble()) },
                        valueRange = PhStatus.MIN.toFloat()..PhStatus.MAX.toFloat(),
                        steps = (((PhStatus.MAX - PhStatus.MIN) / PhStatus.STEP).roundToInt()) - 1,
                        colors = SliderDefaults.colors(
                            thumbColor = status.color,
                            activeTrackColor = status.color,
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                    )
                    RoundIconButton(Icons.Filled.Add, "Increase pH") {
                        value = clampRound(value + PhStatus.STEP)
                    }
                }

                Spacer(Modifier.height(12.dp))

                Eyebrow("When", color = colors.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { showDatePicker = true }) {
                        Text(recordedAt.toLocalDate().format(DateTimeFormatter.ofPattern("d MMM yyyy")), color = ElectricLavender)
                    }
                    TextButton(onClick = { showTimePicker = true }) {
                        Text("%02d:%02d".format(recordedAt.hour, recordedAt.minute), color = ElectricLavender)
                    }
                }

                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { if (it.length <= 500) notes = it },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(12.dp))
                Text(
                    PhCopy.DISCLAIMER,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    (existing ?: PhReading(phValue = value, recordedAt = recordedAt)).copy(
                        phValue = value,
                        recordedAt = recordedAt,
                        notes = notes.ifBlank { null },
                    ),
                )
            }) {
                Text("Save", color = ElectricLavender, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            if (existing != null && onDelete != null) {
                TextButton(onClick = { onDelete(existing.id) }) {
                    Text("Delete", color = colors.error)
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = colors.onSurfaceVariant)
                }
            }
        },
    )

    if (showDatePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = recordedAt.toLocalDate()
                .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        val picked = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        recordedAt = LocalDateTime.of(picked, recordedAt.toLocalTime())
                    }
                    showDatePicker = false
                }) { Text("OK", color = ElectricLavender) }
            },
        ) { DatePicker(state = state) }
    }

    if (showTimePicker) {
        val timeState = rememberTimePickerState(
            initialHour = recordedAt.hour,
            initialMinute = recordedAt.minute,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    recordedAt = recordedAt.withHour(timeState.hour).withMinute(timeState.minute)
                    showTimePicker = false
                }) { Text("OK", color = ElectricLavender) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel", color = colors.onSurfaceVariant)
                }
            },
            text = { TimePicker(state = timeState) },
            containerColor = colors.surface,
        )
    }
}

@Composable
private fun RoundIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(colors.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        IconButton(onClick = onClick) {
            Icon(icon, contentDescription = contentDescription, tint = colors.onSurface)
        }
    }
}

/** Formats a reading timestamp for list/latest rows. */
fun PhReading.formattedWhen(): String = recordedAt.format(whenFormat)
