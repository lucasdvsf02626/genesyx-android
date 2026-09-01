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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.lifecycle.viewModelScope
import com.genesyx.app.data.CycleRepository
import com.genesyx.app.domain.content.phaseLabel
import com.genesyx.app.domain.cycle.CycleEngine
import com.genesyx.app.domain.model.CycleSettings
import com.genesyx.app.data.ConsentRepository
import com.genesyx.app.ui.components.CycleSettingsDialog
import com.genesyx.app.ui.components.Eyebrow
import com.genesyx.app.ui.components.GxPrimaryButton
import com.genesyx.app.ui.insights.OvulationLogic
import com.genesyx.app.ui.profile.SaveState
import com.genesyx.app.ui.profile.toSaveState
import com.genesyx.app.ui.theme.ElectricLavender
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class CycleDetailViewModel @Inject constructor(
    private val cycleRepository: CycleRepository,
    consentRepository: ConsentRepository,
) : ViewModel() {
    val settings: StateFlow<CycleSettings?> = cycleRepository.settings
    val consentActive: StateFlow<Boolean> = consentRepository.isActive

    private val _cycleSave = MutableStateFlow(SaveState())
    val cycleSave: StateFlow<SaveState> = _cycleSave.asStateFlow()

    /** Saves and reports (same shape as ProfileViewModel) — a refused or failed save must not
     *  close the dialog looking exactly like a good one. */
    fun save(settings: CycleSettings) {
        if (_cycleSave.value.saving) return
        _cycleSave.value = SaveState(saving = true)
        viewModelScope.launch { _cycleSave.value = cycleRepository.upsert(settings).toSaveState() }
    }

    fun resetCycleSave() { _cycleSave.value = SaveState() }
}

private val dayMonth: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.UK)

@Composable
fun CycleDetailScreen(
    onBack: () -> Unit,
    viewModel: CycleDetailViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsState()
    val consentActive by viewModel.consentActive.collectAsState()
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
            // Not remembered: a frozen `today` survives past midnight and drifts this screen's phase
            // away from ViewModels that re-evaluate the date. Same reason both cards share one value.
            val today = LocalDate.now()
            val info = CycleEngine.getCyclePhase(settings!!, today)
            val ovulation = OvulationLogic.compute(settings, today)

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
        val cycleSave by viewModel.cycleSave.collectAsState()
        LaunchedEffect(Unit) { viewModel.resetCycleSave() }
        // Close only on a confirmed save — a refused or failed one shows its error in place.
        LaunchedEffect(cycleSave.saved) { if (cycleSave.saved) editing = false }
        CycleSettingsDialog(
            current = settings,
            consentActive = consentActive,
            saving = cycleSave.saving,
            error = cycleSave.error,
            onDismiss = { if (!cycleSave.saving) editing = false },
            onSave = { viewModel.save(it) },
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
