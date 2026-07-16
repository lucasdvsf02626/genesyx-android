package com.genesyx.app.ui.track.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.genesyx.app.data.CycleRepository
import com.genesyx.app.domain.content.phaseLabel
import com.genesyx.app.domain.cycle.CycleEngine
import com.genesyx.app.domain.model.CycleSettings
import com.genesyx.app.ui.components.CycleSettingsDialog
import com.genesyx.app.ui.components.Eyebrow
import com.genesyx.app.ui.components.GxPrimaryButton
import com.genesyx.app.ui.insights.OvulationLogic
import com.genesyx.app.ui.theme.ElectricLavender
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class CycleDetailViewModel @Inject constructor(
    private val cycleRepository: CycleRepository,
) : ViewModel() {
    val settings: StateFlow<CycleSettings?> = cycleRepository.settings
    fun save(settings: CycleSettings) = cycleRepository.upsert(settings)
}

private val dayMonth: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.UK)

@Composable
fun CycleDetailScreen(
    onBack: () -> Unit,
    viewModel: CycleDetailViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsState()
    val colors = MaterialTheme.colorScheme
    var editing by remember { mutableStateOf(false) }

    TrackerDetailScaffold(title = "Cycle", onBack = onBack) {
        Spacer(Modifier.height(8.dp))

        if (settings == null) {
            DetailCard {
                Text("No cycle set up yet", style = MaterialTheme.typography.titleLarge, color = colors.onSurface)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Add when your last period started and your typical cycle length to see your phase, fertile window and ovulation estimate.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(16.dp))
            GxPrimaryButton(text = "Add cycle settings", onClick = { editing = true })
        } else {
            val today = remember { LocalDate.now() }
            val info = CycleEngine.getCyclePhase(settings!!, today)
            val ovulation = OvulationLogic.compute(settings)

            DetailCard {
                Eyebrow("Current phase", color = ElectricLavender)
                Spacer(Modifier.height(6.dp))
                Text(phaseLabel.getValue(info.phase), style = MaterialTheme.typography.headlineSmall, color = colors.onSurface)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Tile("Cycle day", "Day ${info.dayOfCycle}", Modifier.weight(1f))
                    Tile("Cycle length", "${settings!!.cycleLength} days", Modifier.weight(1f))
                    Tile("Period", "${settings!!.periodLength} days", Modifier.weight(1f))
                }
            }

            Spacer(Modifier.height(12.dp))

            DetailCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Prediction", style = MaterialTheme.typography.titleLarge, color = colors.onSurface)
                    Text("Estimated", style = MaterialTheme.typography.bodyMedium, color = ElectricLavender)
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Tile("Ovulation day", ovulation.ovulationDate?.format(dayMonth) ?: "—", Modifier.weight(1f))
                    Tile(
                        "Fertile window",
                        if (ovulation.fertileWindowStart != null && ovulation.fertileWindowEnd != null) {
                            "${ovulation.fertileWindowStart!!.format(dayMonth)} – ${ovulation.fertileWindowEnd!!.format(dayMonth)}"
                        } else "—",
                        Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "These are estimates from your settings — projections, not confirmed measurements.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(16.dp))
            GxPrimaryButton(text = "Edit cycle settings", onClick = { editing = true })
        }

        Spacer(Modifier.height(32.dp))
    }

    if (editing) {
        CycleSettingsDialog(
            current = settings,
            onDismiss = { editing = false },
            onSave = { viewModel.save(it); editing = false },
        )
    }
}

@Composable
private fun Tile(label: String, value: String, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier.clip(RoundedCornerShape(16.dp)).background(colors.surfaceVariant.copy(alpha = 0.4f)).padding(14.dp),
    ) {
        Eyebrow(label, color = colors.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
    }
}

@Composable
private fun DetailCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(20.dp), content = content)
    }
}
