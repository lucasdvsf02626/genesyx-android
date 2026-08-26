package com.genesyx.app.ui.nutrition

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.genesyx.app.data.SupplementWriteResult
import com.genesyx.app.domain.content.supplementPlan
import com.genesyx.app.domain.model.Supplement
import com.genesyx.app.domain.model.SupplementTime
import com.genesyx.app.domain.model.UserSupplement
import com.genesyx.app.ui.components.Eyebrow
import com.genesyx.app.ui.components.GxPrimaryButton
import com.genesyx.app.ui.theme.ElectricBlue
import com.genesyx.app.ui.theme.ElectricLavender
import com.genesyx.app.ui.theme.ElectricPink

/** The colour a plan chip carries — by position, so the four essentials keep their identities. */
internal fun planTint(index: Int): Color = when (index % 4) {
    1 -> ElectricBlue
    3 -> ElectricPink
    else -> ElectricLavender
}

@Composable
internal fun SupplementAvatar(initial: String, index: Int) {
    val tint = planTint(index)
    Box(
        modifier = Modifier.size(28.dp).clip(CircleShape).background(tint.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(initial, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = tint)
    }
}

private const val DEFAULT_REMINDER_MINUTES = 9 * 60 // 09:00

/**
 * "Review Plan" — the plan as a modal sheet (iOS presents the same content the same way).
 *
 * Top: the four bundled essentials, each with a bell for a daily reminder (a local notification,
 * kept device-side by [com.genesyx.app.data.SupplementReminderRepository]). Below: her own
 * supplements, with the add form. Anything added here joins the plan card's chips and its counts.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplementPlanSheet(
    customSupplements: List<UserSupplement>,
    planReminders: Map<Supplement, Int>,
    onSetPlanReminder: (Supplement, Int?) -> Unit,
    onAddSupplement: (UserSupplement) -> SupplementWriteResult,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    fun pickTime(supplement: Supplement) {
        val start = planReminders[supplement] ?: DEFAULT_REMINDER_MINUTES
        android.app.TimePickerDialog(
            context,
            { _, h, m -> onSetPlanReminder(supplement, h * 60 + m) },
            start / 60,
            start % 60,
            android.text.format.DateFormat.is24HourFormat(context),
        ).show()
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = colors.surface) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
        ) {
            Text("Your supplement plan", style = MaterialTheme.typography.titleLarge, color = colors.onSurface)

            Spacer(Modifier.height(20.dp))
            Eyebrow("Genesyx essentials", color = colors.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(
                "Gentle, evidence-informed essentials for fertility prep.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            supplementPlan.forEachIndexed { i, item ->
                val minutes = planReminders[item.supplement]
                Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    SupplementAvatar(item.initial, i)
                    Spacer(Modifier.size(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(item.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = colors.onSurface)
                        Text(item.rationale, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                        if (minutes != null) {
                            Text(
                                "Reminder at ${formatMinutes(minutes)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = ElectricLavender,
                            )
                        }
                    }
                    // The bell: off → pick a time; on → tap turns it off.
                    IconButton(
                        onClick = { if (minutes == null) pickTime(item.supplement) else onSetPlanReminder(item.supplement, null) },
                    ) {
                        Icon(
                            if (minutes != null) Icons.Filled.Notifications else Icons.Outlined.NotificationsNone,
                            contentDescription = if (minutes != null) {
                                "Turn off the ${item.supplement.displayName} reminder"
                            } else {
                                "Set a daily reminder for ${item.supplement.displayName}"
                            },
                            tint = if (minutes != null) ElectricLavender else colors.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Eyebrow("Your supplements", color = colors.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(
                "Add your own supplements to keep everything in one place.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            customSupplements.forEachIndexed { i, entry ->
                Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    SupplementAvatar(entry.name.take(1).uppercase(), supplementPlan.size + i)
                    Spacer(Modifier.size(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(entry.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = colors.onSurface)
                        val detail = listOfNotNull(entry.dose?.takeIf { it.isNotBlank() }, entry.timeOfDay?.label)
                        if (detail.isNotEmpty()) {
                            Text(detail.joinToString(" · "), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            AddSupplementForm(onAdd = onAddSupplement)

            Spacer(Modifier.height(24.dp))
            GxPrimaryButton(text = "Got it", onClick = onDismiss)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSupplementForm(onAdd: (UserSupplement) -> SupplementWriteResult) {
    val colors = MaterialTheme.colorScheme
    var name by remember { mutableStateOf("") }
    var dose by remember { mutableStateOf("") }
    var time by remember { mutableStateOf(SupplementTime.ANYTIME) }
    var timeOpen by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val canAdd = name.trim().isNotEmpty()

    OutlinedTextField(
        value = name,
        onValueChange = { if (it.length <= UserSupplement.NAME_MAX_LENGTH) { name = it; error = null } },
        label = { Text("Name") },
        singleLine = true,
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(10.dp))
    OutlinedTextField(
        value = dose,
        onValueChange = { dose = it },
        label = { Text("Dose") },
        placeholder = { Text("e.g. 400 mcg") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(10.dp))
    ExposedDropdownMenuBox(expanded = timeOpen, onExpandedChange = { timeOpen = it }) {
        OutlinedTextField(
            value = time.label,
            onValueChange = {},
            readOnly = true,
            label = { Text("Time") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = timeOpen) },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = timeOpen, onDismissRequest = { timeOpen = false }) {
            SupplementTime.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = { time = option; timeOpen = false },
                )
            }
        }
    }
    Spacer(Modifier.height(14.dp))
    // Disabled until there is a name — the one thing the server insists on.
    GxPrimaryButton(
        text = "+ Add your own supplement",
        enabled = canAdd,
        onClick = {
            val entry = UserSupplement(name = name.trim(), dose = dose.trim().ifEmpty { null }, timeOfDay = time)
            when (onAdd(entry)) {
                SupplementWriteResult.Accepted -> { name = ""; dose = ""; time = SupplementTime.ANYTIME; error = null }
                SupplementWriteResult.InvalidName -> error = "Give it a name (up to ${UserSupplement.NAME_MAX_LENGTH} characters)."
            }
        },
    )
    Spacer(Modifier.height(4.dp))
    Text(
        "Saved to your Genesyx account.",
        style = MaterialTheme.typography.bodySmall,
        color = colors.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth(),
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
    )
}

/** "9:00 AM" style label for a minutes-of-day reminder time. */
private fun formatMinutes(minutes: Int): String =
    java.time.LocalTime.of((minutes / 60) % 24, minutes % 60)
        .format(java.time.format.DateTimeFormatter.ofPattern("h:mm a"))
