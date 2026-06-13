package com.genesyx.app.data

import com.genesyx.app.domain.model.CycleSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory cycle settings store shared across the app tabs.
 *
 * Stands in for the Supabase-backed `getCycleSettings`/`upsertCycleSettings` server fns
 * (docs/DATA_LAYER.md cycle.functions) until the remote data layer is wired in a later
 * phase. Because it is a Hilt `@Singleton`, Home / Track / Nutrition / Insights all observe
 * the same value, so saving the Cycle Settings dialog updates every screen at once.
 */
@Singleton
class CycleRepository @Inject constructor() {

    private val _settings = MutableStateFlow<CycleSettings?>(null)
    val settings: StateFlow<CycleSettings?> = _settings.asStateFlow()

    fun upsert(settings: CycleSettings) {
        _settings.value = settings
    }
}
