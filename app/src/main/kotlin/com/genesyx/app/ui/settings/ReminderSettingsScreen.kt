package com.genesyx.app.ui.settings

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.genesyx.app.notifications.NotificationPermission
import com.genesyx.app.notifications.PushPermissionStatus
import com.genesyx.app.notifications.model.ReminderKind
import com.genesyx.app.ui.theme.ElectricLavender
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val timeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.UK)
private val dayLetters = listOf("M", "T", "W", "T", "F", "S", "S") // Monday-first, matching WeekBuckets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderSettingsScreen(
    onBack: () -> Unit,
    viewModel: ReminderSettingsViewModel = hiltViewModel(),
) {
    val colors = MaterialTheme.colorScheme
    val settings by viewModel.settings.collectAsState()
    val context = LocalContext.current

    // Permission status is recomputed whenever we return to the screen — returning from system
    // settings must update the banner immediately, or it lies until the user leaves and comes back.
    var refreshKey by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshKey++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
    }
    val status = remember(refreshKey, settings.lastPromptedAt) {
        computeStatus(context, prompted = settings.lastPromptedAt != null)
    }

    var pendingKind by remember { mutableStateOf<ReminderKind?>(null) }
    var showSheet by remember { mutableStateOf(false) }
    var timePickerFor by remember { mutableStateOf<TimeTarget?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            viewModel.onPermissionGranted()
            pendingKind?.let { viewModel.setKindEnabled(it, true) }
        }
        pendingKind = null
        refreshKey++
    }

    fun requestForPendingKind() {
        viewModel.onPrompted()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            // No runtime permission below 33 — grant is implicit, enable straight away.
            viewModel.onPermissionGranted()
            pendingKind?.let { viewModel.setKindEnabled(it, true) }
            pendingKind = null
        }
    }

    fun onToggle(kind: ReminderKind, enabled: Boolean) {
        if (!enabled) {
            viewModel.setKindEnabled(kind, false)
            return
        }
        when (status) {
            PushPermissionStatus.GRANTED, PushPermissionStatus.NOT_REQUIRED ->
                viewModel.setKindEnabled(kind, true)
            PushPermissionStatus.NOT_ASKED, PushPermissionStatus.DENIED_SOFT -> {
                pendingKind = kind
                showSheet = true
            }
            PushPermissionStatus.DENIED_PERMANENT, PushPermissionStatus.BLOCKED_IN_SETTINGS ->
                openNotificationSettings(context) // the banner already explains why
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Reminders") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            val blocked = status == PushPermissionStatus.BLOCKED_IN_SETTINGS ||
                status == PushPermissionStatus.DENIED_PERMANENT
            if (blocked) {
                Spacer(Modifier.height(12.dp))
                BlockedBanner(onOpenSettings = { openNotificationSettings(context) })
            }

            SectionHeader("Tracking")
            SettingsCard {
                ReminderRow(
                    "Daily log reminder",
                    "A nudge to log how you feel",
                    checked = settings.isEnabled(ReminderKind.DAILY_LOG),
                    onCheckedChange = { onToggle(ReminderKind.DAILY_LOG, it) },
                )
                AnimatedVisibility(settings.isEnabled(ReminderKind.DAILY_LOG)) {
                    Column {
                        DetailDivider()
                        TimeRow("Reminder time", settings.dailyLogTime) {
                            timePickerFor = TimeTarget.DAILY
                        }
                        DetailDivider()
                        DayChips(settings.dailyLogDays) { day ->
                            val next = settings.dailyLogDays.toMutableSet().apply {
                                if (day in this) remove(day) else add(day)
                            }
                            viewModel.setDailyDays(next)
                        }
                    }
                }
                DetailDivider()
                ReminderRow(
                    "Missed-log nudge",
                    "If you forget, we'll check in the next morning",
                    checked = settings.isEnabled(ReminderKind.MISSED_LOG),
                    onCheckedChange = { onToggle(ReminderKind.MISSED_LOG, it) },
                )
            }

            SectionHeader("Nutrition & wellness")
            SettingsCard {
                ReminderRow(
                    "Hydration",
                    "A gentle water-break nudge",
                    checked = settings.isEnabled(ReminderKind.HYDRATION),
                    onCheckedChange = { onToggle(ReminderKind.HYDRATION, it) },
                )
                AnimatedVisibility(settings.isEnabled(ReminderKind.HYDRATION)) {
                    Column {
                        DetailDivider()
                        TimeRow("Reminder time", settings.hydrationTime) {
                            timePickerFor = TimeTarget.HYDRATION
                        }
                    }
                }
            }

            SectionHeader("Insights")
            SettingsCard {
                ReminderRow(
                    "Weekly insights",
                    "Sundays at ${settings.weeklyInsightsTime.format(timeFormat)}",
                    checked = settings.isEnabled(ReminderKind.WEEKLY_INSIGHTS),
                    onCheckedChange = { onToggle(ReminderKind.WEEKLY_INSIGHTS, it) },
                )
                AnimatedVisibility(settings.isEnabled(ReminderKind.WEEKLY_INSIGHTS)) {
                    Column {
                        DetailDivider()
                        TimeRow("Reminder time", settings.weeklyInsightsTime) {
                            timePickerFor = TimeTarget.WEEKLY
                        }
                    }
                }
            }

            SectionHeader("Quiet hours")
            SettingsCard {
                ReminderRow(
                    "Quiet hours",
                    "No reminders during these hours",
                    checked = settings.quietHoursEnabled,
                    onCheckedChange = {
                        viewModel.setQuietHours(it, settings.quietHoursStart, settings.quietHoursEnd)
                    },
                )
                AnimatedVisibility(settings.quietHoursEnabled) {
                    Column {
                        DetailDivider()
                        TimeRow("From", settings.quietHoursStart) { timePickerFor = TimeTarget.QUIET_START }
                        DetailDivider()
                        TimeRow("To", settings.quietHoursEnd) { timePickerFor = TimeTarget.QUIET_END }
                    }
                }
            }

            SectionHeader("Occasional check-ins")
            SettingsCard {
                ReminderRow(
                    "Occasional check-ins",
                    "If you've been away for a few days",
                    checked = settings.isEnabled(ReminderKind.REENGAGEMENT),
                    onCheckedChange = { onToggle(ReminderKind.REENGAGEMENT, it) },
                )
            }

            Spacer(Modifier.height(16.dp))
            Text(
                "Reminders are scheduled on your device. Nothing is sent to a server.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            Spacer(Modifier.height(32.dp))
        }
    }

    if (showSheet) {
        PrePermissionSheet(
            onAllow = {
                showSheet = false
                requestForPendingKind()
            },
            onDismiss = {
                showSheet = false
                pendingKind = null
            },
        )
    }

    timePickerFor?.let { target ->
        val initial = when (target) {
            TimeTarget.DAILY -> settings.dailyLogTime
            TimeTarget.HYDRATION -> settings.hydrationTime
            TimeTarget.WEEKLY -> settings.weeklyInsightsTime
            TimeTarget.QUIET_START -> settings.quietHoursStart
            TimeTarget.QUIET_END -> settings.quietHoursEnd
        }
        TimePickerDialog(
            initial = initial,
            onConfirm = { picked ->
                when (target) {
                    TimeTarget.DAILY -> viewModel.setDailyTime(picked)
                    TimeTarget.HYDRATION -> viewModel.setHydrationTime(picked)
                    TimeTarget.WEEKLY -> viewModel.setWeeklyTime(picked)
                    TimeTarget.QUIET_START -> viewModel.setQuietHours(true, picked, settings.quietHoursEnd)
                    TimeTarget.QUIET_END -> viewModel.setQuietHours(true, settings.quietHoursStart, picked)
                }
                timePickerFor = null
            },
            onDismiss = { timePickerFor = null },
        )
    }
}

private enum class TimeTarget { DAILY, HYDRATION, WEEKLY, QUIET_START, QUIET_END }

@Composable
private fun SectionHeader(text: String) {
    Spacer(Modifier.height(20.dp))
    Text(
        text.uppercase(Locale.UK),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column { content() }
    }
}

@Composable
private fun ReminderRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = ElectricLavender),
        )
    }
}

@Composable
private fun TimeRow(label: String, time: LocalTime, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        Text(time.format(timeFormat), style = MaterialTheme.typography.bodyLarge, color = ElectricLavender, fontWeight = FontWeight.Medium)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DayChips(selected: Set<DayOfWeek>, onToggle: (DayOfWeek) -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        DayOfWeek.entries.forEachIndexed { index, day ->
            FilterChip(
                selected = day in selected,
                onClick = { onToggle(day) },
                label = { Text(dayLetters[index]) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ElectricLavender.copy(alpha = 0.25f)),
            )
        }
    }
}

@Composable
private fun DetailDivider() {
    Spacer(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
    )
}

@Composable
private fun BlockedBanner(onOpenSettings: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Notifications are off in your phone's settings. Turn them on to get reminders.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onOpenSettings, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                Text("Open settings", color = ElectricLavender, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initial: LocalTime,
    onConfirm: (LocalTime) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute, is24Hour = false)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(LocalTime.of(state.hour, state.minute)) }) {
                Text("Set", color = ElectricLavender)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        text = { TimePicker(state = state) },
    )
}

// ── Permission plumbing ─────────────────────────────────────────────────────

private fun computeStatus(context: Context, prompted: Boolean): PushPermissionStatus {
    val api = Build.VERSION.SDK_INT
    val enabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
    val granted = if (api >= NotificationPermission.RUNTIME_PERMISSION_API) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
    val activity = context.findActivity()
    val rationale = api >= NotificationPermission.RUNTIME_PERMISSION_API && activity != null &&
        ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.POST_NOTIFICATIONS)
    return NotificationPermission.evaluate(api, granted, enabled, rationale, prompted)
}

private fun openNotificationSettings(context: Context) {
    val direct = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    runCatching { context.startActivity(direct) }.onFailure {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
            )
        }
    }
}

private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
