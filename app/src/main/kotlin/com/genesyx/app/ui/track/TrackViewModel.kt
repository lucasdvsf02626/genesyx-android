package com.genesyx.app.ui.track

import androidx.lifecycle.ViewModel
import com.genesyx.app.data.CycleRepository
import com.genesyx.app.data.PhRepository
import com.genesyx.app.domain.model.CycleSettings
import com.genesyx.app.domain.model.PhReading
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class TrackViewModel @Inject constructor(
    private val cycleRepository: CycleRepository,
    private val phRepository: PhRepository,
) : ViewModel() {

    val settings: StateFlow<CycleSettings?> = cycleRepository.settings
    val readings: StateFlow<List<PhReading>> = phRepository.readings

    fun saveCycleSettings(settings: CycleSettings) = cycleRepository.upsert(settings)

    fun savePhReading(reading: PhReading) = phRepository.create(reading)
    fun updatePhReading(reading: PhReading) = phRepository.update(reading)
    fun deletePhReading(id: String) = phRepository.delete(id)
}
