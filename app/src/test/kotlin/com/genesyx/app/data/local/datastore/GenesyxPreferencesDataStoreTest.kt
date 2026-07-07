package com.genesyx.app.data.local.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.genesyx.app.domain.model.ThemeMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class GenesyxPreferencesDataStoreTest {
    @get:Rule val tmp = TemporaryFolder()

    private var fileCounter = 0

    /** A DataStore-backed prefs store over a fresh temp file (one per call → no file-lock clash). */
    private fun newStore(): GenesyxPreferencesDataStore {
        val ds: DataStore<Preferences> = PreferenceDataStoreFactory.create(
            produceFile = { File(tmp.root, "prefs_${fileCounter++}.preferences_pb") },
        )
        return GenesyxPreferencesDataStore(ds)
    }

    @Test
    fun `theme default is LIGHT`() = runTest {
        assertEquals(ThemeMode.LIGHT, newStore().themeMode.first())
    }

    @Test
    fun `theme choice round-trips through DataStore for all modes`() = runTest {
        val store = newStore()
        for (mode in ThemeMode.entries) {
            store.setTheme(mode)
            assertEquals(mode, store.themeMode.first())
        }
    }
}
