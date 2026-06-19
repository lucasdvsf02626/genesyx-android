package com.genesyx.app.ui.track

import androidx.lifecycle.ViewModel
import com.genesyx.app.data.CycleRepository
import com.genesyx.app.domain.model.CycleSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class TrackViewModel @Inject constructor(
    private val cycleRepository: CycleRepository,
) : ViewModel() {

    val settings: StateFlow<CycleSettings?> = cycleRepository.settings

    fun saveCycleSettings(settings: CycleSettings) = cycleRepository.upsert(settings)
}
