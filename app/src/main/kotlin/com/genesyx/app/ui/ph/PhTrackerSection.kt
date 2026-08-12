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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.genesyx.app.data.PhRepository
import com.genesyx.app.data.PreferencesRepository
import com.genesyx.app.domain.model.PhReading
import com.genesyx.app.domain.ph.PhCopy
import com.genesyx.app.ui.components.Eyebrow
import com.genesyx.app.ui.components.PhLogDialog
import com.genesyx.app.ui.components.PhReadingRow
import com.genesyx.app.ui.components.PhTrackerCard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class PhTrackerViewModel @Inject constructor(
    private val phRepository: PhRepository,
    private val preferences: PreferencesRepository,
) : ViewModel() {
    val readings: StateFlow<List<PhReading>> = phRepository.readings
    /** One-time "the tracker now records vaginal pH" notice — shown until dismissed once. */
    val vaginalNoticeSeen: StateFlow<Boolean> = preferences.phVaginalNoticeSeen
    fun save(reading: PhReading) = phRepository.create(reading)
    fun update(reading: PhReading) = phRepository.update(reading)
    fun delete(id: String) = phRepository.delete(id)
    fun dismissVaginalNotice() = preferences.setPhVaginalNoticeSeen(true)
}

/**
 * Self-contained vaginal-pH card + log dialog, plus the one-time notice announcing the switch from
 * urine to vaginal pH (shown once, gated by a DataStore flag). Rendered by the pH tab, which passes
 * [showHistory] so every previous reading is listed and editable, not just the latest one on the
 * card.
 */
@Composable
fun PhTrackerSection(
    modifier: Modifier = Modifier,
    showHistory: Boolean = false,
    viewModel: PhTrackerViewModel = hiltViewModel(),
) {
    val readings by viewModel.readings.collectAsState()
    val noticeSeen by viewModel.vaginalNoticeSeen.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<PhReading?>(null) }

    if (!noticeSeen) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissVaginalNotice() },
            title = { Text(PhCopy.NOTICE_TITLE, style = MaterialTheme.typography.titleLarge) },
            text = { Text(PhCopy.NOTICE_BODY, style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissVaginalNotice() }) { Text(PhCopy.NOTICE_DISMISS) }
            },
        )
    }

    PhTrackerCard(
        readings = readings,
        onLogClick = { editing = null; showDialog = true },
        modifier = modifier,
    )

    if (showHistory && readings.isNotEmpty()) {
        Spacer(Modifier.height(16.dp))
        PhHistoryCard(readings = readings, onEdit = { editing = it; showDialog = true })
    }

    if (showDialog) {
        PhLogDialog(
            existing = editing,
            onDismiss = { showDialog = false },
            onSave = { reading ->
                if (editing == null) viewModel.save(reading) else viewModel.update(reading)
                showDialog = false
            },
            onDelete = { id ->
                viewModel.delete(id)
                showDialog = false
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
