package com.genesyx.app.ui.track.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.genesyx.app.data.DailyLogRepository
import com.genesyx.app.domain.model.DailyLog
import com.genesyx.app.ui.components.Eyebrow
import com.genesyx.app.ui.components.GxPrimaryButton
import com.genesyx.app.ui.insights.SleepInsightLogic
import com.genesyx.app.ui.theme.BabyLavender
import com.genesyx.app.ui.theme.PowderBlue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import androidx.lifecycle.viewModelScope
import java.time.LocalDate
import javax.inject.Inject

data class SleepDetailState(
    val todayMinutes: Int = 0,
    val bars: List<Int> = List(7) { 0 },
    val nightlyAverageMinutes: Int? = null,
    val nightsLogged: Int = 0,
    val insight: String = "",
    val hasData: Boolean = false,
)

@HiltViewModel
class SleepDetailViewModel @Inject constructor(
    private val dailyLogRepository: DailyLogRepository,
) : ViewModel() {
    val uiState: StateFlow<SleepDetailState> = dailyLogRepository.logByDate.map { logs ->
        val today = LocalDate.now()
        val insights = SleepInsightLogic.compute(logs, today)
        SleepDetailState(
            todayMinutes = logs[today]?.sleepMinutes ?: 0,
            bars = insights.bars,
            nightlyAverageMinutes = insights.nightlyAverageMinutes,
            nightsLogged = insights.nightsLogged,
            insight = insights.insight,
            hasData = insights.hasData,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SleepDetailState())

    /** Writes tonight's sleep through the shared log — Insights and the tracker row update from it. */
    fun setSleep(minutes: Int) {
        val today = LocalDate.now()
        val current: DailyLog = dailyLogRepository.logOn(today)
        dailyLogRepository.upsert(today, current.copy(sleepMinutes = minutes.coerceIn(0, 14 * 60)))
    }
}

private val weekdayLabels = listOf("M", "T", "W", "T", "F", "S", "S")

@Composable
fun SleepDetailScreen(
    onBack: () -> Unit,
    viewModel: SleepDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val colors = MaterialTheme.colorScheme

    // Editor draft, seeded from the stored value. `-1` marks "not yet touched" so we can seed once.
    var draft by remember(state.todayMinutes) { mutableIntStateOf(state.todayMinutes) }

    TrackerDetailScaffold(title = "Sleep", onBack = onBack) {
        Spacer(Modifier.height(8.dp))

        // ── This week
        TrackerDetailCard {
            Text("This week", style = MaterialTheme.typography.titleLarge, color = colors.onSurface)
            Spacer(Modifier.height(12.dp))
            if (state.hasData) {
                SleepBars(state.bars)
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Tile("Nightly average", state.nightlyAverageMinutes?.let { SleepInsightLogic.formatDuration(it) } ?: "—", Modifier.weight(1f))
                    Tile("Nights logged", "${state.nightsLogged}/7", Modifier.weight(1f))
                }
                Spacer(Modifier.height(14.dp))
                Text(state.insight, style = MaterialTheme.typography.bodyMedium, color = colors.onSurface.copy(alpha = 0.8f))
            } else {
                Text(state.insight, style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(12.dp))

        // ── Editor
        TrackerDetailCard {
            Eyebrow("Last night", color = colors.onSurfaceVariant)
            Spacer(Modifier.height(10.dp))
            Text(SleepInsightLogic.formatDuration(draft), style = MaterialTheme.typography.headlineMedium, color = colors.onSurface)
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Stepper("Hours", Icons.Filled.Remove, Icons.Filled.Add,
                    onMinus = { draft = (draft - 60).coerceAtLeast(0) },
                    onPlus = { draft = (draft + 60).coerceAtMost(14 * 60) })
                Stepper("Minutes", Icons.Filled.Remove, Icons.Filled.Add,
                    onMinus = { draft = (draft - 15).coerceAtLeast(0) },
                    onPlus = { draft = (draft + 15).coerceAtMost(14 * 60) })
            }
            Spacer(Modifier.height(16.dp))
            GxPrimaryButton(text = "Save sleep", onClick = { viewModel.setSleep(draft) })
            Spacer(Modifier.height(6.dp))
            Text(
                "Saved to your log and synced when you're online. An unlogged night stays empty, never a zero-hour night.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun SleepBars(bars: List<Int>) {
    val colors = MaterialTheme.colorScheme
    Column {
        Row(modifier = Modifier.fillMaxWidth().height(96.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
            bars.forEach { v ->
                if (v <= 0) {
                    Box(Modifier.weight(1f).height(2.dp).clip(CircleShape).background(colors.onSurfaceVariant.copy(alpha = 0.25f)))
                } else {
                    Box(Modifier.weight(1f).fillMaxHeight((v / 100f).coerceIn(0.02f, 1f)).clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)).background(Brush.verticalGradient(listOf(BabyLavender, PowderBlue))))
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            weekdayLabels.forEach { Text(it, modifier = Modifier.weight(1f), fontSize = 10.sp, color = colors.onSurfaceVariant, textAlign = TextAlign.Center) }
        }
    }
}

@Composable
private fun Stepper(label: String, minus: ImageVector, plus: ImageVector, onMinus: () -> Unit, onPlus: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Eyebrow(label, color = colors.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StepBtn(minus, "Lower $label", onMinus)
            StepBtn(plus, "Raise $label", onPlus)
        }
    }
}

@Composable
private fun StepBtn(icon: ImageVector, cd: String, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier.size(44.dp).clip(CircleShape).background(colors.surfaceVariant).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, cd, tint = colors.onSurface, modifier = Modifier.size(20.dp)) }
}

@Composable
private fun Tile(label: String, value: String, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(modifier = modifier.clip(RoundedCornerShape(16.dp)).background(colors.surfaceVariant.copy(alpha = 0.4f)).padding(14.dp)) {
        Eyebrow(label, color = colors.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
    }
}
