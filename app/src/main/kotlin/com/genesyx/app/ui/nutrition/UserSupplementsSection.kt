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
import androidx.compose.material3.Switch
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
import com.genesyx.app.domain.model.GenesyxProduct
import com.genesyx.app.domain.model.SupplementTime
import com.genesyx.app.domain.model.UserSupplement
import com.genesyx.app.ui.components.Eyebrow
import com.genesyx.app.ui.components.GxPrimaryButton
import com.genesyx.app.ui.theme.ElectricLavender

/**
 * "Your supplements" — the user's own entries (manual supplement entry), plus "The Genesyx range"
 * catalogue card. Entries live in `user_supplements` (offline-first, synced); the catalogue is the
 * read-only `genesyx_products` table, which renders a "coming soon" line while it has no rows —
 * seeding products later needs no app change.
 */
@Composable
fun UserSupplementsCard(
    supplements: List<UserSupplement>,
    reminders: Map<String, Int>,
    onSave: (UserSupplement) -> Unit,
    onDelete: (UserSupplement) -> Unit,
    onSetReminder: (id: String, name: String, minutesOfDay: Int?) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    // null = closed; an entry with a blank name = the add flow; otherwise editing that entry.
    var editing by remember { mutableStateOf<UserSupplement?>(null) }
    var confirmDelete by remember { mutableStateOf<UserSupplement?>(null) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(20.dp)) {
            Eyebrow("Your supplements", color = ElectricLavender)
            Spacer(Modifier.height(8.dp))
            if (supplements.isEmpty()) {
                Text(
                    "Add the supplements you take — your own, in your own words.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
            } else {
                supplements.forEach { entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { editing = entry }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                entry.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = colors.onSurface,
                            )
                            val reminderLabel = reminders[entry.id]?.let { "Reminder ${formatMinutes(it)}" }
                            val detail = listOfNotNull(entry.dose, entry.timeOfDay?.label, reminderLabel)
                            if (detail.isNotEmpty()) {
                                Text(
                                    detail.joinToString(" · "),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.onSurfaceVariant,
                                )
                            }
                        }
                        IconButton(onClick = { confirmDelete = entry }) {
                            Icon(
                                Icons.Outlined.Delete,
                                "Delete ${entry.name}",
                                tint = colors.onSurfaceVariant,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            GxPrimaryButton(text = "Add supplement", onClick = { editing = UserSupplement(name = "") })
        }
    }

    editing?.let { entry ->
        UserSupplementDialog(
            initial = entry,
            initialReminderMinutes = reminders[entry.id],
            onDismiss = { editing = null },
            onSave = { supplement, reminderMinutes ->
                onSave(supplement)
                onSetReminder(supplement.id, supplement.name.trim(), reminderMinutes)
                editing = null
            },
        )
    }

    confirmDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            shape = RoundedCornerShape(20.dp),
            containerColor = colors.surface,
            title = { Text("Delete ${entry.name}?", style = MaterialTheme.typography.titleLarge, color = colors.onSurface) },
            text = {
                Text(
                    "This removes it from your list on every device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
            },
            confirmButton = {
                TextButton(onClick = { onDelete(entry); confirmDelete = null }) {
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

/** Add/edit one supplement: free-text name (1..60), dose, time of day, and an optional daily
 *  reminder (a local notification — see SupplementReminderScheduler). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UserSupplementDialog(
    initial: UserSupplement,
    initialReminderMinutes: Int?,
    onDismiss: () -> Unit,
    onSave: (UserSupplement, reminderMinutes: Int?) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val context = androidx.compose.ui.platform.LocalContext.current
    val isNew = initial.name.isBlank()
    var name by remember { mutableStateOf(initial.name) }
    var dose by remember { mutableStateOf(initial.dose.orEmpty()) }
    var time by remember { mutableStateOf(initial.timeOfDay) }
    // null = no reminder. Defaults to 9:00 the first time it's switched on.
    var reminderMinutes by remember { mutableStateOf(initialReminderMinutes) }
    val nameValid = name.trim().length in 1..UserSupplement.NAME_MAX_LENGTH

    fun pickTime() {
        val start = reminderMinutes ?: DEFAULT_REMINDER_MINUTES
        android.app.TimePickerDialog(
            context,
            { _, h, m -> reminderMinutes = h * 60 + m },
            start / 60,
            start % 60,
            android.text.format.DateFormat.is24HourFormat(context),
        ).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = colors.surface,
        title = {
            Text(
                if (isNew) "Add a supplement" else "Edit supplement",
                style = MaterialTheme.typography.titleLarge,
                color = colors.onSurface,
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= UserSupplement.NAME_MAX_LENGTH) name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = dose,
                    onValueChange = { dose = it },
                    label = { Text("Dose (optional)") },
                    placeholder = { Text("e.g. 400 mcg") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                Eyebrow("When you take it", color = colors.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SupplementTime.entries.forEach { option ->
                        val selected = time == option
                        Text(
                            option.label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (selected) ElectricLavender else colors.onSurfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) ElectricLavender.copy(alpha = 0.08f) else Color.Transparent)
                                .border(
                                    1.dp,
                                    if (selected) ElectricLavender else colors.outline,
                                    RoundedCornerShape(12.dp),
                                )
                                // Tapping the picked option clears it — the field is optional.
                                .clickable { time = if (selected) null else option }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                // Optional daily reminder — a local notification at a time she picks. Approximate
                // (WorkManager), never promised to the minute.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Daily reminder", style = MaterialTheme.typography.bodyLarge, color = colors.onSurface)
                        Text(
                            reminderMinutes?.let { "At ${formatMinutes(it)}" } ?: "Off",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = reminderMinutes != null,
                        onCheckedChange = { on -> reminderMinutes = if (on) DEFAULT_REMINDER_MINUTES else null },
                    )
                }
                if (reminderMinutes != null) {
                    TextButton(onClick = { pickTime() }, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                        Text("Change time", color = ElectricLavender)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = nameValid,
                onClick = {
                    onSave(initial.copy(name = name.trim(), dose = dose, timeOfDay = time), reminderMinutes)
                },
            ) { Text("Save", color = if (nameValid) ElectricLavender else colors.onSurfaceVariant) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = colors.onSurfaceVariant) }
        },
    )
}

private const val DEFAULT_REMINDER_MINUTES = 9 * 60 // 09:00

/** "9:00 AM" style label for a minutes-of-day reminder time, in the device's locale. */
private fun formatMinutes(minutes: Int): String =
    java.time.LocalTime.of((minutes / 60) % 24, minutes % 60)
        .format(java.time.format.DateTimeFormatter.ofPattern("h:mm a"))

/** The Genesyx range, read from the shared catalogue. Empty (today's truth: zero SKUs) or signed
 *  out → "coming soon"; rows appear here the day they are seeded, with no app update. */
@Composable
fun GenesyxRangeCard(
    products: List<GenesyxProduct>,
    addedProductIds: Set<String>,
    onAdd: (GenesyxProduct) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(20.dp)) {
            Eyebrow("The Genesyx range", color = ElectricLavender)
            Spacer(Modifier.height(8.dp))
            if (products.isEmpty()) {
                Text(
                    "Genesyx supplements are coming soon. They'll appear here the moment they launch.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
            } else {
                products.forEach { product ->
                    val added = product.id in addedProductIds
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                product.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = colors.onSurface,
                            )
                            product.dose?.let {
                                Text(it, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                            }
                        }
                        TextButton(onClick = { onAdd(product) }, enabled = !added) {
                            Text(
                                if (added) "Added" else "Add",
                                color = if (added) colors.onSurfaceVariant else ElectricLavender,
                            )
                        }
                    }
                }
            }
        }
    }
}
