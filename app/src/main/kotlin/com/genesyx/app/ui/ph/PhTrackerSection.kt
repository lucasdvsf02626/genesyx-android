package com.genesyx.app.ui.ph

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.genesyx.app.data.PhRepository
import com.genesyx.app.domain.model.PhReading
import com.genesyx.app.ui.components.Eyebrow
import com.genesyx.app.ui.components.PhLogDialog
import com.genesyx.app.ui.components.PhReadingRow
import com.genesyx.app.ui.components.PhTrackerCard
import com.genesyx.app.ui.profile.SaveState
import com.genesyx.app.ui.profile.toSaveState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class PhTrackerViewModel @Inject constructor(
    private val phRepository: PhRepository,
) : ViewModel() {
    val readings: StateFlow<List<PhReading>> = phRepository.readings

    private val _saveState = MutableStateFlow(SaveState())
    val saveState: StateFlow<SaveState> = _saveState.asStateFlow()

    fun save(reading: PhReading, isNew: Boolean) {
        if (_saveState.value.saving) return
        _saveState.value = SaveState(saving = true)
        viewModelScope.launch {
            val result = if (isNew) phRepository.create(reading) else phRepository.update(reading)
            _saveState.value = result.toSaveState()
        }
    }

    fun resetSave() { _saveState.value = SaveState() }

    fun delete(id: String) = phRepository.delete(id)
}

/**
 * Self-contained vaginal-pH card + log dialog. Rendered by the pH tab, which passes [showHistory]
 * so every previous reading is listed and editable, not just the latest one on the card.
 */
@Composable
fun PhTrackerSection(
    modifier: Modifier = Modifier,
    showHistory: Boolean = false,
    viewModel: PhTrackerViewModel = hiltViewModel(),
) {
    val readings by viewModel.readings.collectAsState()
    val save by viewModel.saveState.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<PhReading?>(null) }

    // Reset here rather than in the dialog's own LaunchedEffect: reopening before recomposition
    // settles would otherwise show the previous attempt's error, or close on its stale success.
    fun open(reading: PhReading?) {
        viewModel.resetSave()
        editing = reading
        showDialog = true
    }

    PhTrackerCard(
        readings = readings,
        onLogClick = { open(null) },
        modifier = modifier,
    )

    if (showHistory && readings.isNotEmpty()) {
        Spacer(Modifier.height(16.dp))
        PhHistoryCard(readings = readings, onEdit = { open(it) })
    }

    if (showDialog) {
        // Close on the save landing, not on the button being pressed — a reading the consent gate
        // refused persists nothing, and closing on press reported it as saved.
        LaunchedEffect(save.saved) { if (save.saved) { showDialog = false; viewModel.resetSave() } }
        PhLogDialog(
            existing = editing,
            error = save.error,
            saving = save.saving,
            onDismiss = { showDialog = false; viewModel.resetSave() },
            onSave = { reading -> viewModel.save(reading, isNew = editing == null) },
            onDelete = { id ->
                viewModel.delete(id)
                showDialog = false
                viewModel.resetSave()
            },
        )
    }
}

/**
 * Every reading, newest first, grouped by day — the chart shows the shape, this shows the record.
 * Tapping a row opens the same [PhLogDialog] prefilled, so a mistyped value from last week is
 * fixable without hunting for it on the calendar. Legacy urine rows render exactly as elsewhere.
 */
@Composable
private fun PhHistoryCard(readings: List<PhReading>, onEdit: (PhReading) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val byDay = remember(readings) {
        readings.sortedByDescending { it.recordedAt }.groupBy { it.recordedAt.toLocalDate() }
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(colors.surface)
            .padding(20.dp),
    ) {
        Text("Previous readings", style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
        Text(
            "Tap a reading to edit it.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
        )
        byDay.forEach { (date, rows) ->
            Spacer(Modifier.height(12.dp))
            Eyebrow(date.format(historyDateFormat), color = colors.onSurfaceVariant)
            rows.forEach { reading ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onEdit(reading) }
                        .padding(vertical = 6.dp, horizontal = 2.dp),
                ) {
                    PhReadingRow(reading)
                }
            }
        }
    }
}

private val historyDateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d MMM yyyy")
