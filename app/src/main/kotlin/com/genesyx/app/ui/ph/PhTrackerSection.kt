package com.genesyx.app.ui.ph

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
import com.genesyx.app.domain.model.PhReading
import com.genesyx.app.ui.components.PhLogDialog
import com.genesyx.app.ui.components.PhTrackerCard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class PhTrackerViewModel @Inject constructor(
    private val phRepository: PhRepository,
) : ViewModel() {
    val readings: StateFlow<List<PhReading>> = phRepository.readings
    fun save(reading: PhReading) = phRepository.create(reading)
    fun update(reading: PhReading) = phRepository.update(reading)
    fun delete(id: String) = phRepository.delete(id)
}

/**
 * Self-contained urine-pH card + log dialog (mirrors the web `PhTrackerCard`, which manages its own
 * data via `usePhReadings`/`usePhMutations`). Embedded on both Track and Nutrition.
 */
@Composable
fun PhTrackerSection(
    modifier: Modifier = Modifier,
    viewModel: PhTrackerViewModel = hiltViewModel(),
) {
    val readings by viewModel.readings.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<PhReading?>(null) }

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
