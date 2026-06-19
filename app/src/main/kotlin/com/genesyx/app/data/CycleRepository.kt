package com.genesyx.app.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.genesyx.app.data.local.CycleSettingsDto
import com.genesyx.app.data.local.toDomain
import com.genesyx.app.data.local.toDto
import com.genesyx.app.domain.model.CycleSettings
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

/**
 * Cycle settings, persisted on-device via DataStore (local-only v1). An in-memory StateFlow keeps
 * the synchronous read API the screens rely on; it hydrates from disk on first use and writes back
 * on every change. Replaces the Supabase `getCycleSettings`/`upsertCycleSettings` for now.
 */
@Singleton
class CycleRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val json: Json,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val key = stringPreferencesKey("cycle_settings")

    private val _settings = MutableStateFlow<CycleSettings?>(null)
    val settings: StateFlow<CycleSettings?> = _settings.asStateFlow()

    init {
        scope.launch {
            dataStore.data.first()[key]?.let { raw ->
                runCatching { json.decodeFromString<CycleSettingsDto>(raw).toDomain() }
                    .getOrNull()?.let { _settings.value = it }
            }
        }
    }

    fun upsert(settings: CycleSettings) {
        _settings.value = settings
        scope.launch { dataStore.edit { it[key] = json.encodeToString(settings.toDto()) } }
    }
}
