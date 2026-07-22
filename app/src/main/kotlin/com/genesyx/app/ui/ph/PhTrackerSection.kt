package com.genesyx.app.ui.ph

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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.genesyx.app.data.PhRepository
import com.genesyx.app.data.PreferencesRepository
import com.genesyx.app.domain.model.PhReading
import com.genesyx.app.domain.ph.PhCopy
import com.genesyx.app.ui.components.PhLogDialog
import com.genesyx.app.ui.components.PhTrackerCard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
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
 * urine to vaginal pH (shown once, gated by a DataStore flag). Embedded on both Track and Nutrition.
 */
@Composable
fun PhTrackerSection(
    modifier: Modifier = Modifier,
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
