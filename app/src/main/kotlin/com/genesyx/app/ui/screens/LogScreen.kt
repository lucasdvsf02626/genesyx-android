package com.genesyx.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.material.icons.filled.SentimentNeutral
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.genesyx.app.domain.content.AppGuide
import com.genesyx.app.domain.hydration.HydrationFormat
import com.genesyx.app.domain.model.DailyLog
import com.genesyx.app.domain.model.EnergyLevel
import com.genesyx.app.domain.model.Mood
import com.genesyx.app.domain.model.Supplement
import com.genesyx.app.domain.model.SupplementLogRows
import com.genesyx.app.ui.components.ConsentWithdrawnBanner
import com.genesyx.app.ui.components.Eyebrow
import com.genesyx.app.ui.components.GxPrimaryButton
import com.genesyx.app.ui.components.ScreenHeader
import com.genesyx.app.ui.learn.HowThisWorksLink
import com.genesyx.app.ui.nutrition.FoodGroupCard
import com.genesyx.app.ui.nutrition.MealLogCard
import com.genesyx.app.ui.theme.ElectricBlue
import com.genesyx.app.ui.theme.ElectricLavender
import com.genesyx.app.ui.theme.ElectricPink
import java.time.format.DateTimeFormatter

private val DEFAULT_SYMPTOMS = listOf("Headache", "Fatigue", "Cramps", "Nausea", "Bloating", "Acne", "Backache", "Tender breasts")

private val moodIcons = mapOf(
    Mood.GREAT to Icons.Filled.Favorite,
    Mood.GOOD to Icons.Filled.SentimentSatisfied,
    Mood.OKAY to Icons.Filled.SentimentNeutral,
    Mood.LOW to Icons.Filled.SentimentDissatisfied,
)

@Composable
fun LogScreen(
    onClose: () -> Unit,
    onOpenArticle: (String) -> Unit = {},
    viewModel: LogViewModel = hiltViewModel(),
) {
    val loaded by viewModel.loaded.collectAsState()

    // Seeding the form from an unloaded store would show blanks over a real log, and saving that
    // form would overwrite it. Wait for Room instead.
    if (!loaded) {
        Column(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        ) {
            ScreenHeader(title = logTitle(viewModel), subtitle = logSubtitle(viewModel), onBack = onClose)
        }
        return
    }

    LogForm(
        initial = viewModel.initialLog(),
        onClose = onClose,
        onOpenArticle = onOpenArticle,
        viewModel = viewModel,
    )
}

// The header must say WHICH day is being written: an editor opened from the calendar that still
// read "Log Today" would have users editing last Tuesday believing it was today.
private val logDayFormat = DateTimeFormatter.ofPattern("EEEE, d MMM")

private fun logTitle(viewModel: LogViewModel) =
    if (viewModel.isToday) "Log Today" else viewModel.date.format(logDayFormat)

private fun logSubtitle(viewModel: LogViewModel) =
    if (viewModel.isToday) "Quick notes about how you're feeling." else "Add or edit this day's log."

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LogForm(
    initial: DailyLog,
    onClose: () -> Unit,
    onOpenArticle: (String) -> Unit,
    viewModel: LogViewModel,
) {
    val colors = MaterialTheme.colorScheme

    var mood by remember { mutableStateOf(initial.mood) }
    var energy by remember { mutableStateOf(initial.energy) }
    var symptoms by remember { mutableStateOf(initial.symptoms) }
    var notes by remember { mutableStateOf(initial.notes.orEmpty()) }
    var sleepMinutes by remember { mutableStateOf(initial.sleepMinutes) }
    var supplements by remember { mutableStateOf(initial.supplements) }
    // Tri-state on the wire (null = not recorded), two-state here: the switch records or clears.
    var intimacy by remember { mutableStateOf(initial.sexualActivity) }
    // Water is not form state: the quick-add trackers own it, so the mini-card renders the live
    // aggregate and the dialog writes straight through the repository. A snapshot here used to
    // revert any water quick-added while the form sat open.
    val waterMl by viewModel.waterMl.collectAsState()
    val waterUnit by viewModel.hydrationUnit.collectAsState()
    val customSupplements by viewModel.customSupplementNames.collectAsState()
    val foodGroups by viewModel.foodGroups.collectAsState()
    val meals by viewModel.meals.collectAsState()
    val consentActive by viewModel.consentActive.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var custom by remember { mutableStateOf("") }

    var sleepOpen by remember { mutableStateOf(false) }
    var waterOpen by remember { mutableStateOf(false) }
    var suppOpen by remember { mutableStateOf(false) }
    var confirmDiscard by remember { mutableStateOf(false) }

    val allSymptoms = remember(symptoms) { (DEFAULT_SYMPTOMS + symptoms).distinct() }

    // Water is excluded from the dirty check: it saves the moment the dialog closes, and the
    // repository preserves the stored total on save regardless of what this snapshot carries.
    val edited = DailyLog(
        mood = mood,
        energy = energy,
        symptoms = symptoms,
        sleepMinutes = sleepMinutes,
        supplements = supplements,
        // Carried through untouched, like water: this form cannot edit food groups (iOS logs them),
        // and rebuilding the log without them would delete them from the shared row on every save.
        foodGroups = initial.foodGroups,
        notes = notes.ifBlank { null },
        waterMl = initial.waterMl,
        sexualActivity = intimacy,
    )
    val dirty = edited != initial

    // Leaving with unsaved edits used to bin them silently. Ask first — but only when there is
    // something to lose, so an untouched form still closes on the first tap.
    fun attemptClose() { if (dirty) confirmDiscard = true else onClose() }
    BackHandler(enabled = dirty) { confirmDiscard = true }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState()),
    ) {
        ScreenHeader(title = logTitle(viewModel), subtitle = logSubtitle(viewModel), onBack = ::attemptClose)

        Column(Modifier.padding(horizontal = 20.dp)) {
            if (!consentActive) {
                Spacer(Modifier.height(16.dp))
                ConsentWithdrawnBanner()
            }

            // Mood
            Section("Mood")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Mood.entries.forEach { m ->
                    val sel = mood == m
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 76.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (sel) ElectricLavender.copy(alpha = 0.08f) else colors.surface)
                            .border(1.dp, if (sel) ElectricLavender else colors.outline, RoundedCornerShape(16.dp))
                            .clickable { mood = m }
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(moodIcons.getValue(m), null, tint = if (sel) ElectricLavender else colors.onSurface.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                        Spacer(Modifier.height(4.dp))
                        Text(m.label, fontSize = 11.5.sp, fontWeight = FontWeight.Medium, color = if (sel) ElectricLavender else colors.onSurface.copy(alpha = 0.8f))
                    }
                }
            }

            // Energy
            Section("Energy")
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(colors.surfaceVariant).padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                EnergyLevel.entries.forEach { e ->
                    val sel = energy == e
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (sel) colors.surface else Color.Transparent)
                            .clickable { energy = e }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(e.id.replaceFirstChar { it.uppercase() }, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = if (sel) colors.onSurface else colors.onSurfaceVariant)
                    }
                }
            }

            // Symptoms
            Section("Symptoms")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                allSymptoms.forEach { s ->
                    val sel = s in symptoms
                    Row(
                        modifier = Modifier
                            .heightIn(min = 36.dp)
                            .clip(CircleShape)
                            .background(if (sel) ElectricLavender else colors.surface)
                            .border(1.dp, if (sel) ElectricLavender else colors.outline, CircleShape)
                            .clickable { symptoms = if (sel) symptoms - s else symptoms + s }
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (sel) {
                            Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.size(4.dp))
                        }
                        Text(s, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = if (sel) Color.White else colors.onSurface.copy(alpha = 0.8f))
                    }
                }
                if (showAdd) {
                    OutlinedTextField(
                        value = custom,
                        onValueChange = { custom = it },
                        modifier = Modifier.heightIn(min = 36.dp),
                        placeholder = { Text("Add symptom") },
                        singleLine = true,
                    )
                    TextButton(onClick = {
                        if (custom.isNotBlank()) symptoms = symptoms + custom.trim()
                        custom = ""; showAdd = false
                    }) { Text("Add", color = ElectricLavender) }
                } else {
                    Row(
                        modifier = Modifier
                            .heightIn(min = 36.dp)
                            .clip(CircleShape)
                            .border(1.dp, colors.outline, CircleShape)
                            .clickable { showAdd = true }
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.Add, null, tint = colors.onSurfaceVariant, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.size(4.dp))
                        Text("Add", fontSize = 13.sp, color = colors.onSurfaceVariant)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            // Mini-cards
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MiniCard(Icons.Filled.Bedtime, "Sleep", sleepMinutes?.let { "${it / 60}h ${it % 60}m" } ?: "—", ElectricLavender, Modifier.weight(1f)) { sleepOpen = true }
                MiniCard(Icons.Outlined.WaterDrop, "Water", if (waterMl > 0) HydrationFormat.format(waterMl, waterUnit) else "—", ElectricBlue, Modifier.weight(1f)) { waterOpen = true }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MiniCard(Icons.Filled.Medication, "Supplements", "${supplements.size} of ${Supplement.loggable.size + customSupplements.size}", ElectricLavender, Modifier.weight(1f)) { suppOpen = true }
            }

            // Intimacy — deliberately quiet: one switch, neutral words, and the privacy statement
            // right where the record is made. Never shared with any partner feature.
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.surface)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(ElectricPink.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Favorite, null, tint = ElectricPink, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.size(12.dp))
                Column(Modifier.weight(1f)) {
                    Eyebrow("Intimacy", color = colors.onSurfaceVariant)
                    Text("Private to you", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                }
                Switch(
                    checked = intimacy == true,
                    onCheckedChange = { intimacy = if (it) true else null },
                    colors = SwitchDefaults.colors(checkedTrackColor = ElectricPink),
                )
            }

            Spacer(Modifier.height(16.dp))
            FoodGroupCard(
                logged = foodGroups,
                phase = null,
                onToggle = { viewModel.toggleFoodGroup(it) },
            )
            Spacer(Modifier.height(12.dp))
            MealLogCard(
                meals = meals,
                date = viewModel.date,
                onLog = { viewModel.logMeal(it) },
                onDelete = { viewModel.deleteMeal(it) },
            )

            // Notes
            Section("Notes")
            OutlinedTextField(
                value = notes,
                onValueChange = { if (it.length <= 2000) notes = it },
                modifier = Modifier.fillMaxWidth().heightIn(min = 96.dp),
                placeholder = { Text("A short note for future you…") },
                shape = RoundedCornerShape(16.dp),
            )

            HowThisWorksLink(
                slug = AppGuide.TRACK,
                label = AppGuide.TRACK_LABEL,
                onOpen = onOpenArticle,
            )

            Spacer(Modifier.height(20.dp))
            GxPrimaryButton(
                text = "Save log",
                onClick = {
                    // Saves unconditionally, online or off: the write lands in Room and, if the push
                    // fails, queues for a WorkManager retry. Nothing is lost, so nothing is blocked.
                    viewModel.save(edited)
                    onClose()
                },
            )
            Spacer(Modifier.height(24.dp))
        }
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            shape = RoundedCornerShape(20.dp),
            containerColor = colors.surface,
            title = { Text("Discard your changes?", style = MaterialTheme.typography.titleLarge, color = colors.onSurface) },
            text = {
                Text(
                    "This log hasn't been saved yet. Leaving now loses what you've entered.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
            },
            confirmButton = {
                TextButton(onClick = { confirmDiscard = false; onClose() }) {
                    Text("Discard", color = colors.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDiscard = false }) {
                    Text("Keep editing", color = ElectricLavender)
                }
            },
        )
    }

    if (sleepOpen) {
        SleepDialog(initialMinutes = sleepMinutes, onDismiss = { sleepOpen = false }, onDone = { sleepMinutes = it; sleepOpen = false })
    }
    if (waterOpen) {
        // Saves immediately through the shared repository — every screen shows it at once.
        WaterDialog(initialMl = waterMl, onDismiss = { waterOpen = false }, onDone = { viewModel.setWater(it); waterOpen = false })
    }
    if (suppOpen) {
        SupplementsDialog(selected = supplements, custom = customSupplements, onToggle = { supplements = if (it in supplements) supplements - it else supplements + it }, onDismiss = { suppOpen = false })
    }
}

@Composable
private fun Section(title: String) {
    Spacer(Modifier.height(16.dp))
    Eyebrow(title, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
}

@Composable
private fun MiniCard(icon: ImageVector, label: String, value: String, tint: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surface)
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(tint.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.height(10.dp))
        Eyebrow(label, color = colors.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
    }
}

@Composable
private fun SleepDialog(initialMinutes: Int?, onDismiss: () -> Unit, onDone: (Int) -> Unit) {
    var hours by remember { mutableStateOf(((initialMinutes ?: 420) / 60).toString()) }
    var mins by remember { mutableStateOf(((initialMinutes ?: 420) % 60).toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Sleep") },
        text = {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(hours, { hours = it.filter(Char::isDigit) }, label = { Text("Hours") }, singleLine = true, modifier = Modifier.weight(1f))
                OutlinedTextField(mins, { mins = it.filter(Char::isDigit) }, label = { Text("Minutes") }, singleLine = true, modifier = Modifier.weight(1f))
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val h = (hours.toIntOrNull() ?: 0).coerceIn(0, 24)
                val m = (mins.toIntOrNull() ?: 0).coerceIn(0, 59)
                onDone(h * 60 + m)
            }) { Text("Done", color = ElectricLavender) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant) } },
    )
}

@Composable
private fun WaterDialog(initialMl: Int, onDismiss: () -> Unit, onDone: (Int) -> Unit) {
    var input by remember { mutableStateOf(initialMl.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Water (ml)") },
        text = {
            OutlinedTextField(
                input,
                { input = it.filter(Char::isDigit) },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onDone((input.toIntOrNull() ?: 0).coerceIn(0, 10000)) }) { Text("Done", color = ElectricLavender) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant) } },
    )
}

@Composable
private fun SupplementsDialog(selected: Set<String>, custom: List<String>, onToggle: (String) -> Unit, onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme

    @Composable
    fun SupplementRow(display: String, stored: String) {
        // Toggles the stored string, shows the display name. The set she edits is the set that
        // is stored and synced, so the strings crossing that boundary never change.
        val checked = stored in selected
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (checked) ElectricLavender.copy(alpha = 0.08f) else Color.Transparent)
                .border(1.dp, if (checked) ElectricLavender else colors.outline, RoundedCornerShape(12.dp))
                .clickable { onToggle(stored) }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(display, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = colors.onSurface, modifier = Modifier.weight(1f))
            if (checked) Icon(Icons.Filled.Check, null, tint = ElectricLavender, modifier = Modifier.size(18.dp))
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = colors.surface,
        title = { Text("Supplements") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                // Built-ins, then the user's own entries, then any already-logged string that no
                // longer matches a current entry (renamed/other device) — so nothing logged is
                // ever hidden or un-untoggleable. See SupplementLogRows.
                SupplementLogRows.forDay(selected, custom).forEach { row ->
                    SupplementRow(display = row.display, stored = row.stored)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done", color = ElectricLavender) } },
    )
}
