package com.genesyx.app.data

import com.genesyx.app.domain.model.PhReading
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

/**
 * In-memory urine-pH store, ordered by `recordedAt` ascending.
 *
 * Stands in for `listPhReadings`/`create`/`update`/`deletePhReading` (docs/DATA_LAYER.md
 * ph.functions) until the remote data layer lands. pH values are rounded to 1 dp on write,
 * matching the web `round(v*10)/10` behaviour.
 */
@Singleton
class PhRepository @Inject constructor() {

    private val _readings = MutableStateFlow<List<PhReading>>(emptyList())
    val readings: StateFlow<List<PhReading>> = _readings.asStateFlow()

    private fun Double.round1(): Double = (this * 10).roundToInt() / 10.0

    fun create(reading: PhReading) {
        val normalized = reading.copy(phValue = reading.phValue.round1())
        _readings.value = (_readings.value + normalized).sortedBy { it.recordedAt }
    }

    fun update(reading: PhReading) {
        val normalized = reading.copy(phValue = reading.phValue.round1())
        _readings.value = _readings.value
            .map { if (it.id == reading.id) normalized else it }
            .sortedBy { it.recordedAt }
    }

    fun delete(id: String) {
        _readings.value = _readings.value.filterNot { it.id == id }
    }
}
