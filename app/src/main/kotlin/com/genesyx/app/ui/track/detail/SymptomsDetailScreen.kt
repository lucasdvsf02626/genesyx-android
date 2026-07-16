package com.genesyx.app.ui.track.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.genesyx.app.data.DailyLogRepository
import com.genesyx.app.ui.components.Eyebrow
import com.genesyx.app.ui.components.GxPrimaryButton
import com.genesyx.app.ui.insights.SymptomPatternLogic
import com.genesyx.app.ui.theme.ElectricLavender
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

data class SymptomsDetailState(
    val heatmap: List<Int> = List(SymptomPatternLogic.WINDOW_DAYS) { 0 },
    val insight: String = "",
    val hasData: Boolean = false,
    /** Days in the window that carried symptoms, newest first: date → symptom names. */
    val recent: List<Pair<LocalDate, List<String>>> = emptyList(),
)

@HiltViewModel
class SymptomsDetailViewModel @Inject constructor(
    dailyLogRepository: DailyLogRepository,
) : ViewModel() {
    val uiState: StateFlow<SymptomsDetailState> = dailyLogRepository.logByDate.map { logs ->
        val today = LocalDate.now()
        val insights = SymptomPatternLogic.compute(logs, today)
        val recent = (0 until SymptomPatternLogic.WINDOW_DAYS)
            .map { today.minusDays(it.toLong()) }
            .mapNotNull { date -> logs[date]?.symptoms?.takeIf { it.isNotEmpty() }?.let { date to it.sorted() } }
        SymptomsDetailState(
            heatmap = insights.heatmapValues,
            insight = insights.insight,
            hasData = recent.isNotEmpty(),
            recent = recent,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SymptomsDetailState())
}

private val entryDay: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d MMM", Locale.UK)

@Composable
fun SymptomsDetailScreen(
    onBack: () -> Unit,
    onEditToday: () -> Unit,
    onOpenHistory: () -> Unit,
    viewModel: SymptomsDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val colors = MaterialTheme.colorScheme

    TrackerDetailScaffold(title = "Symptoms", onBack = onBack) {
        Spacer(Modifier.height(8.dp))

        TrackerDetailCard {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.Bottom) {
                Text("Last 4 weeks", style = MaterialTheme.typography.titleLarge, color = colors.onSurface)
            }
            Spacer(Modifier.height(16.dp))
            state.heatmap.chunked(7).forEach { week ->
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    week.forEach { count ->
                        Box(
                            Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(8.dp)).background(
                                when {
                                    count <= 0 -> colors.surfaceVariant.copy(alpha = 0.45f)
                                    count == 1 -> ElectricLavender.copy(alpha = 0.35f)
                                    count == 2 -> ElectricLavender.copy(alpha = 0.6f)
                                    else -> ElectricLavender.copy(alpha = 0.9f)
                                },
                            ),
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(state.insight, style = MaterialTheme.typography.bodyMedium, color = colors.onSurface.copy(alpha = 0.8f))
        }

        Spacer(Modifier.height(12.dp))

        if (state.hasData) {
            TrackerDetailCard {
                Text("Recent entries", style = MaterialTheme.typography.titleLarge, color = colors.onSurface)
                Spacer(Modifier.height(4.dp))
                Text("Tap to review a day in your log history.", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                state.recent.forEach { (date, symptoms) ->
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onOpenHistory).padding(vertical = 8.dp),
                    ) {
                        Text(date.format(entryDay), style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
                        Text(symptoms.joinToString(", "), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        GxPrimaryButton(text = "Log how you feel", onClick = onEditToday)
        Spacer(Modifier.height(32.dp))
    }
}
