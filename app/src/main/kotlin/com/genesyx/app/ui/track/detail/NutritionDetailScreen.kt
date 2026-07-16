package com.genesyx.app.ui.track.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.genesyx.app.data.DailyLogRepository
import com.genesyx.app.domain.model.Supplement
import com.genesyx.app.ui.components.Eyebrow
import com.genesyx.app.ui.components.GxPrimaryButton
import com.genesyx.app.ui.insights.SupplementInsightLogic
import com.genesyx.app.ui.theme.ElectricLavender
import com.genesyx.app.ui.theme.PowderBlue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

data class NutritionDetailState(
    val takenToday: Int = 0,
    val planSize: Int = Supplement.defaultPlan.size,
    val bars: List<Int> = List(7) { 0 },
    val daysLogged: Int = 0,
    val suppTotal: Int = 0,
    val insight: String = "",
    val hasData: Boolean = false,
)

@HiltViewModel
class NutritionDetailViewModel @Inject constructor(
    dailyLogRepository: DailyLogRepository,
) : ViewModel() {
    val uiState: StateFlow<NutritionDetailState> = dailyLogRepository.logByDate.map { logs ->
        val today = LocalDate.now()
        val plan = Supplement.defaultPlan
        val takenToday = logs[today]?.supplements.orEmpty().mapNotNull(Supplement::fromWire).count { it in plan }
        val insights = SupplementInsightLogic.compute(logs, today)
        NutritionDetailState(
            takenToday = takenToday,
            planSize = plan.size,
            bars = insights.bars,
            daysLogged = insights.daysLogged,
            suppTotal = insights.suppTotal,
            insight = insights.insight,
            hasData = insights.hasData,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NutritionDetailState())
}

private val weekdayLabels = listOf("M", "T", "W", "T", "F", "S", "S")

@Composable
fun NutritionDetailScreen(
    onBack: () -> Unit,
    onLog: () -> Unit,
    viewModel: NutritionDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val colors = MaterialTheme.colorScheme

    TrackerDetailScaffold(title = "Nutrition", onBack = onBack) {
        Spacer(Modifier.height(8.dp))

        TrackerDetailCard {
            Eyebrow("Supplements today", color = colors.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            Text(
                "${state.takenToday} of ${state.planSize}",
                style = MaterialTheme.typography.headlineMedium,
                color = colors.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                if (state.takenToday > 0) "Logged from your plan today." else "Nothing from your plan logged today yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(12.dp))

        TrackerDetailCard {
            Text("This week", style = MaterialTheme.typography.titleLarge, color = colors.onSurface)
            Spacer(Modifier.height(12.dp))
            if (state.hasData) {
                Row(modifier = Modifier.fillMaxWidth().height(96.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                    state.bars.forEach { v ->
                        if (v <= 0) {
                            Box(Modifier.weight(1f).height(2.dp).clip(CircleShape).background(colors.onSurfaceVariant.copy(alpha = 0.25f)))
                        } else {
                            Box(Modifier.weight(1f).fillMaxHeight((v / 100f).coerceIn(0.02f, 1f)).clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)).background(Brush.verticalGradient(listOf(ElectricLavender, PowderBlue))))
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    weekdayLabels.forEach { Text(it, modifier = Modifier.weight(1f), fontSize = 10.sp, color = colors.onSurfaceVariant, textAlign = TextAlign.Center) }
                }
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Tile("Days logged", "${state.daysLogged}/7", Modifier.weight(1f))
                    Tile("Supplements taken", "${state.suppTotal}", Modifier.weight(1f))
                }
                Spacer(Modifier.height(14.dp))
                Text(state.insight, style = MaterialTheme.typography.bodyMedium, color = colors.onSurface.copy(alpha = 0.8f))
            } else {
                Text(state.insight, style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(16.dp))
        GxPrimaryButton(text = "Log supplements", onClick = onLog)
        Spacer(Modifier.height(32.dp))
    }
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
