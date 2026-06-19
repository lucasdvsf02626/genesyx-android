package com.genesyx.app.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.genesyx.app.data.local.PhReadingDto
import com.genesyx.app.data.local.toDomain
import com.genesyx.app.data.local.toDto
import com.genesyx.app.domain.model.PhReading
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

/**
 * Urine-pH readings, persisted on-device via DataStore, ordered by `recordedAt` ascending.
 * pH values are rounded to 1 dp on write (web `round(v*10)/10`).
 */
@Singleton
class PhRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val json: Json,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val key = stringPreferencesKey("ph_readings")

    private val _readings = MutableStateFlow<List<PhReading>>(emptyList())
    val readings: StateFlow<List<PhReading>> = _readings.asStateFlow()

    init {
        scope.launch {
            dataStore.data.first()[key]?.let { raw ->
                runCatching { json.decodeFromString<List<PhReadingDto>>(raw) }
                    .getOrNull()?.let { stored ->
                        _readings.value = stored.map { it.toDomain() }.sortedBy { r -> r.recordedAt }
                    }
            }
        }
    }

    private fun Double.round1(): Double = (this * 10).roundToInt() / 10.0

    fun create(reading: PhReading) {
        val normalized = reading.copy(phValue = reading.phValue.round1())
        _readings.value = (_readings.value + normalized).sortedBy { it.recordedAt }
        persist()
    }

    fun update(reading: PhReading) {
        val normalized = reading.copy(phValue = reading.phValue.round1())
        _readings.value = _readings.value
            .map { if (it.id == reading.id) normalized else it }
            .sortedBy { it.recordedAt }
        persist()
    }

    fun delete(id: String) {
        _readings.value = _readings.value.filterNot { it.id == id }
        persist()
    }

    private fun persist() {
        val snapshot = _readings.value.map { it.toDto() }
        scope.launch { dataStore.edit { it[key] = json.encodeToString(snapshot) } }
    }
}
