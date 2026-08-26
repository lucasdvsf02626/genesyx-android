package com.genesyx.app.ui.track.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.genesyx.app.ui.components.Eyebrow
import com.genesyx.app.ui.theme.ElectricLavender
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class NutritionDetailViewModel @Inject constructor(
    dailyLogRepository: DailyLogRepository,
) : ViewModel() {
    /** Reads the same rows the Nutrition tab's chips write — one repository, no second path. */
    val uiState: StateFlow<NutritionTrackerSummary> = dailyLogRepository.logByDate
        .map { logs -> NutritionTrackerLogic.compute(logs) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NutritionTrackerSummary())
}

private val weekdayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
private val weekdayNames = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

const val NUTRITION_EMPTY_ENTRY = "No entries yet — log today to start"
const val NUTRITION_WEEK_EMPTY_NOTE =
    "No supplements logged yet this week — even one, whenever you remember, is a gentle start."

/**
 * The Nutrition tracker, reached from Track's "Your trackers" list. A read-only summary of what
 * the Nutrition tab logged — iOS presents it as a sheet with Done; on Android it is a pushed
 * detail with Back, the platform's equivalent (ANDROID_PARITY.md: don't port a sheet literally).
 */
@Composable
fun NutritionDetailScreen(
    onBack: () -> Unit,
    viewModel: NutritionDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val colors = MaterialTheme.colorScheme

    TrackerDetailScaffold(title = "Nutrition", onBack = onBack) {
        Spacer(Modifier.height(8.dp))

        TodayCard(title = "Supplements from today's log", items = state.todaySupplements)
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
