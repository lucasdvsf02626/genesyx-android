package com.genesyx.app.ui.track.detail

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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.genesyx.app.data.DailyLogRepository
import com.genesyx.app.data.LogWriteResult
import com.genesyx.app.data.SupplementReminderRepository
import com.genesyx.app.data.SupplementWriteResult
import com.genesyx.app.data.UserSupplementRepository
import com.genesyx.app.domain.model.Supplement
import com.genesyx.app.domain.model.SupplementPlanEntry
import com.genesyx.app.domain.model.SupplementToggleSet
import com.genesyx.app.domain.model.UserSupplement
import com.genesyx.app.ui.components.Eyebrow
import com.genesyx.app.ui.components.GxPrimaryButton
import com.genesyx.app.ui.nutrition.SupplementPlanSheet
import com.genesyx.app.ui.nutrition.SupplementSaveEvent
import com.genesyx.app.ui.theme.ElectricLavender
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class NutritionDetailViewModel @Inject constructor(
    private val dailyLogRepository: DailyLogRepository,
    private val userSupplementRepository: UserSupplementRepository,
    private val supplementReminderRepository: SupplementReminderRepository,
) : ViewModel() {
    /** Reads the same rows the Nutrition tab's chips write — one repository, no second path. */
    val uiState: StateFlow<NutritionTrackerSummary> = dailyLogRepository.logByDate
        .map { logs -> NutritionTrackerLogic.compute(logs) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NutritionTrackerSummary())

    /** Today's stored supplement names, as written — the checklist ticks against this. */
    val loggedToday: StateFlow<Set<String>> = dailyLogRepository.logByDate
        .map { logs -> logs[LocalDate.now()]?.supplements.orEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    /** The same toggle set as the Nutrition tab's plan card: four essentials, then her own. */
    val planEntries: StateFlow<List<SupplementPlanEntry>> = userSupplementRepository.supplements
        .map { SupplementToggleSet.build(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, SupplementToggleSet.build(emptyList()))

    val userSupplements: StateFlow<List<UserSupplement>> = userSupplementRepository.supplements

    /** supplement id → daily reminder time (minutes-of-day); the sheet's bells. */
    val supplementReminders: StateFlow<Map<String, Int>> = supplementReminderRepository.reminders

    val planReminders: StateFlow<Map<Supplement, Int>> = supplementReminderRepository.reminders
        .map { all ->
            Supplement.defaultPlan.mapNotNull { s ->
                all[SupplementReminderRepository.planReminderId(s)]?.let { s to it }
            }.toMap()
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    private val _supplementEvents = MutableSharedFlow<SupplementSaveEvent>(extraBufferCapacity = 8)

    /** Anything a checklist tap has to say beyond the tick — refused, queued, failed. */
    val supplementEvents: SharedFlow<SupplementSaveEvent> = _supplementEvents.asSharedFlow()

    /** Same write path as the Nutrition tab's chips — one repository, one result contract. */
    fun toggleSupplement(entry: SupplementPlanEntry) {
        viewModelScope.launch {
            when (val result = dailyLogRepository.toggleSupplement(entry.stored)) {
                LogWriteResult.Saved -> Unit
                LogWriteResult.Queued -> _supplementEvents.emit(SupplementSaveEvent.Queued(entry))
                LogWriteResult.Refused -> _supplementEvents.emit(SupplementSaveEvent.Refused(entry))
                is LogWriteResult.Failed -> _supplementEvents.emit(SupplementSaveEvent.Failed(entry, result.message))
            }
        }
    }

    suspend fun saveSupplement(entry: UserSupplement): SupplementWriteResult =
        if (entry.id in userSupplements.value.map { it.id }) userSupplementRepository.update(entry)
        else userSupplementRepository.create(entry)

    fun setPlanReminder(supplement: Supplement, minutesOfDay: Int?) {
        val id = SupplementReminderRepository.planReminderId(supplement)
        if (minutesOfDay == null) supplementReminderRepository.clearReminder(id)
        else supplementReminderRepository.setReminder(id, supplement.displayName, minutesOfDay)
    }

    fun setSupplementReminder(id: String, name: String, minutesOfDay: Int?) {
        if (minutesOfDay == null) supplementReminderRepository.clearReminder(id)
        else supplementReminderRepository.setReminder(id, name, minutesOfDay)
    }
}

private val weekdayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
private val weekdayNames = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

const val NUTRITION_EMPTY_ENTRY = "No entries yet — log today to start"
const val NUTRITION_WEEK_EMPTY_NOTE =
    "No supplements logged yet this week — even one, whenever you remember, is a gentle start."

/**
 * The Nutrition tracker, reached from Track's "Your trackers" list. Today's supplements and food
 * groups, the week's dots — and, so it is never a dead end, the supplement checklist itself:
 * every entry by name (the four essentials, then her own), tap to log or un-log for today, and a
 * button into the same plan sheet the Nutrition tab opens (review, reminders, add your own).
 * iOS presents this as a sheet with Done; on Android it is a pushed detail with Back
 * (ANDROID_PARITY.md: don't port a sheet literally).
 */
@Composable
fun NutritionDetailScreen(
    onBack: () -> Unit,
    viewModel: NutritionDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val loggedToday by viewModel.loggedToday.collectAsState()
    val planEntries by viewModel.planEntries.collectAsState()
    val userSupplements by viewModel.userSupplements.collectAsState()
    val planReminders by viewModel.planReminders.collectAsState()
    val supplementReminders by viewModel.supplementReminders.collectAsState()
    val colors = MaterialTheme.colorScheme
    var planOpen by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }

    // A tap that did not simply save says so — refused, queued offline, or failed (Retry).
    LaunchedEffect(Unit) {
        viewModel.supplementEvents.collect { event ->
            val result = snackbar.showSnackbar(
                message = event.text,
                actionLabel = if (event is SupplementSaveEvent.Failed) "Retry" else null,
                withDismissAction = true,
                duration = if (event is SupplementSaveEvent.Queued) SnackbarDuration.Short else SnackbarDuration.Long,
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.toggleSupplement(event.entry)
        }
    }

    TrackerDetailScaffold(title = "Nutrition", onBack = onBack, snackbarHost = { SnackbarHost(snackbar) }) {
        Spacer(Modifier.height(8.dp))

        TodayCard(title = "Supplements from today's log", items = state.todaySupplements)

        // ── The logging surface: tick a supplement to log it for today. Same write path, same
        // toggle set and the same "N of M" as the Nutrition tab's chips.
        Spacer(Modifier.height(12.dp))
        SupplementChecklistCard(
            entries = planEntries,
            loggedToday = loggedToday,
            onToggle = { viewModel.toggleSupplement(it) },
            onOpenPlan = { planOpen = true },
        )

        Spacer(Modifier.height(12.dp))
        TodayCard(title = "Food groups from today's log", items = state.todayFoodGroups)

        Spacer(Modifier.height(12.dp))
        TrackerDetailCard {
            Eyebrow("Supplements this week", color = colors.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            WeekDotStrip(counts = state.weekSupplementCounts, what = "supplements")
        }

        Spacer(Modifier.height(12.dp))
        TrackerDetailCard {
            Eyebrow("Food groups this week", color = colors.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            WeekDotStrip(counts = state.weekFoodGroupCounts, what = "food groups")
        }

        // Only when the week is genuinely empty — a week with one logged day gets no nudge.
        if (state.supplementWeekEmpty) {
            Spacer(Modifier.height(16.dp))
            Text(
                NUTRITION_WEEK_EMPTY_NOTE,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(32.dp))
    }

    if (planOpen) {
        SupplementPlanSheet(
            customSupplements = userSupplements,
            planReminders = planReminders,
            customReminders = supplementReminders,
            onSetPlanReminder = { supplement, minutes -> viewModel.setPlanReminder(supplement, minutes) },
            onSetCustomReminder = { entry, minutes -> viewModel.setSupplementReminder(entry.id, entry.name, minutes) },
            onAddSupplement = { viewModel.saveSupplement(it) },
            onDismiss = { planOpen = false },
        )
    }
}

/**
 * "Log supplements" — one row per entry, by name, with a checkbox. The four essentials first,
 * then her own under "Your supplements"; the button below opens the plan sheet (review the
 * essentials, set reminders, add your own).
 */
@Composable
private fun SupplementChecklistCard(
    entries: List<SupplementPlanEntry>,
    loggedToday: Set<String>,
    onToggle: (SupplementPlanEntry) -> Unit,
    onOpenPlan: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val taken = SupplementToggleSet.takenCount(entries, loggedToday)
    val essentials = entries.filter { !it.isCustom }
    val own = entries.filter { it.isCustom }
    TrackerDetailCard {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Eyebrow("Log supplements", color = colors.onSurfaceVariant)
            Text(
                SupplementToggleSet.statusLine(taken, entries.size),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (taken == 0) colors.onSurfaceVariant else ElectricLavender,
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "Tick what you've taken today. It saves straight away.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        essentials.forEach { entry ->
            SupplementCheckRow(entry, SupplementToggleSet.isLogged(entry, loggedToday)) { onToggle(entry) }
        }
        Spacer(Modifier.height(10.dp))
        Text("Your supplements", style = MaterialTheme.typography.titleSmall, color = colors.onSurface)
        if (own.isEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(
                "None added yet — add your own from the supplement plan below.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
        } else {
            own.forEach { entry ->
                SupplementCheckRow(entry, SupplementToggleSet.isLogged(entry, loggedToday)) { onToggle(entry) }
            }
        }
        Spacer(Modifier.height(14.dp))
        GxPrimaryButton(text = "Supplement plan", onClick = onOpenPlan)
        TextButton(onClick = onOpenPlan, modifier = Modifier.fillMaxWidth()) {
            Text("Review the essentials, set reminders, add your own", style = MaterialTheme.typography.bodySmall, color = ElectricLavender)
        }
    }
}

@Composable
private fun SupplementCheckRow(entry: SupplementPlanEntry, logged: Boolean, onToggle: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val label = "${entry.display}, ${if (logged) "logged" else "not logged"} today"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .toggleable(value = logged, role = Role.Checkbox, onValueChange = { onToggle() })
            .semantics { contentDescription = label }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = logged,
            onCheckedChange = null, // the whole row is the toggle
            colors = CheckboxDefaults.colors(checkedColor = ElectricLavender),
        )
        Column(Modifier.weight(1f)) {
            Text(entry.display, style = MaterialTheme.typography.bodyLarge, color = colors.onSurface)
            entry.dose?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant) }
        }
    }
}

@Composable
private fun TodayCard(title: String, items: List<String>) {
    val colors = MaterialTheme.colorScheme
    TrackerDetailCard {
        Eyebrow("Today", color = colors.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
        Spacer(Modifier.height(6.dp))
        if (items.isEmpty()) {
            Text(NUTRITION_EMPTY_ENTRY, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        } else {
            Text(
                items.joinToString(" · "),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = colors.onSurface,
            )
        }
    }
}

/** Seven columns, Mon–Sun: a dot (filled when anything was logged) and the count or a dash. */
@Composable
private fun WeekDotStrip(counts: List<Int>, what: String) {
    // "1 supplement" / "2 supplements", "1 food group" / "2 food groups".
    fun plural(n: Int) = if (n == 1) what.removeSuffix("s") else what
    val colors = MaterialTheme.colorScheme
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        counts.forEachIndexed { i, count ->
            val logged = count > 0
            Column(
                modifier = Modifier
                    .weight(1f)
                    .semantics {
                        contentDescription =
                            if (logged) "${weekdayNames[i]}: $count ${plural(count)}" else "${weekdayNames[i]}: none"
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(weekdayLabels[i], fontSize = 10.sp, color = colors.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Box(
                    Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(if (logged) ElectricLavender else colors.surfaceVariant.copy(alpha = 0.5f)),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    if (logged) "$count" else "–",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (logged) colors.onSurface else colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
