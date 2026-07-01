package com.genesyx.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.genesyx.app.ui.components.Eyebrow
import com.genesyx.app.ui.components.GxBackButton
import com.genesyx.app.ui.components.GxOptionPill
import com.genesyx.app.ui.components.GxPrimaryButton
import com.genesyx.app.ui.theme.ElectricLavender
import kotlinx.coroutines.launch

private val moods = listOf("😊 Great", "🙂 Good", "😐 Okay", "😔 Low", "😣 Rough")
private val energyOptions = listOf("Low", "Normal", "High")
private val symptomOptions = listOf(
    "Cramps", "Bloating", "Headache", "Breast tenderness",
    "Mood swings", "Fatigue", "Spotting", "Back pain", "Nausea", "Insomnia",
)
private val supplementOptions = listOf(
    "Folic acid", "Vitamin D", "Iron", "Omega-3", "CoQ10", "Magnesium", "Zinc", "B6",
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LogScreen(
    onClose: () -> Unit,
    viewModel: LogViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme

    LaunchedEffect(state.saved) {
        if (state.saved) {
            snackbar.showSnackbar("Today's log saved ✓")
            onClose()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            GxBackButton(onClick = onClose)
            Text(
                "Log today",
                style = MaterialTheme.typography.titleLarge,
                color = colors.onBackground,
                modifier = Modifier.padding(start = 8.dp),
            )
        }

        Spacer(Modifier.height(20.dp))
        Eyebrow("Mood")
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            moods.forEach { mood ->
                FilterChip(
                    selected = state.mood == mood,
                    onClick = { viewModel.onMoodChange(mood) },
                    label = { Text(mood) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ElectricLavender.copy(alpha = 0.15f),
                        selectedLabelColor = ElectricLavender,
                    ),
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Eyebrow("Energy")
        Spacer(Modifier.height(8.dp))
        energyOptions.forEach { e ->
            GxOptionPill(text = e, selected = state.energy == e, onClick = { viewModel.onEnergyChange(e) })
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(8.dp))
        Eyebrow("Symptoms")
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            symptomOptions.forEach { s ->
                FilterChip(
                    selected = s in state.symptoms,
                    onClick = { viewModel.toggleSymptom(s) },
                    label = { Text(s) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ElectricLavender.copy(alpha = 0.15f),
                        selectedLabelColor = ElectricLavender,
                    ),
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Eyebrow("Water intake")
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { viewModel.onWaterChange(-250) }) {
                Icon(Icons.Filled.Remove, null, tint = ElectricLavender)
            }
            Text(
                "${state.waterMl} ml",
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurface,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            IconButton(onClick = { viewModel.onWaterChange(+250) }) {
                Icon(Icons.Filled.Add, null, tint = ElectricLavender)
            }
        }

        Spacer(Modifier.height(16.dp))
        Eyebrow("Sleep (hours)")
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { viewModel.onSleepChange(-30) }) {
                Icon(Icons.Filled.Remove, null, tint = ElectricLavender)
            }
            val hours = state.sleepMinutes / 60
            val mins = state.sleepMinutes % 60
            Text(
                if (mins == 0) "${hours}h" else "${hours}h ${mins}m",
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurface,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            IconButton(onClick = { viewModel.onSleepChange(+30) }) {
                Icon(Icons.Filled.Add, null, tint = ElectricLavender)
            }
        }

        Spacer(Modifier.height(16.dp))
        Eyebrow("Supplements")
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            supplementOptions.forEach { s ->
                FilterChip(
                    selected = s in state.supplements,
                    onClick = { viewModel.toggleSupplement(s) },
                    label = { Text(s) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ElectricLavender.copy(alpha = 0.15f),
                        selectedLabelColor = ElectricLavender,
                    ),
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = state.notes,
            onValueChange = viewModel::onNotesChange,
            label = { Text("Notes (optional)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
        )

        Spacer(Modifier.height(24.dp))
        GxPrimaryButton(text = "Save today's log", onClick = viewModel::save, enabled = !state.isSaving)
        Spacer(Modifier.height(24.dp))

        SnackbarHost(snackbar)
    }
}
