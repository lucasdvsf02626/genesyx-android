package com.genesyx.app.data.local.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.genesyx.app.domain.model.ThemeMode
import com.genesyx.app.domain.streaks.StreakEngine
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

    @Test
    fun `hydration goal defaults to the engine's default until she sets one`() = runTest {
        assertEquals(StreakEngine.DEFAULT_GOAL_ML, newStore().hydrationGoalMl.first())
    }

    @Test
    fun `hydration goal round-trips through DataStore`() = runTest {
        val store = newStore()
        store.setHydrationGoalMl(3000)
        assertEquals(3000, store.hydrationGoalMl.first())
    }

    @Test
    fun `ph vaginal notice defaults to unseen so it fires once`() = runTest {
        assertEquals(false, newStore().phVaginalNoticeSeen.first())
    }

    @Test
    fun `dismissing the ph vaginal notice persists so it does not re-fire`() = runTest {
        val store = newStore()
        store.setPhVaginalNoticeSeen(true)
        assertEquals(true, store.phVaginalNoticeSeen.first())
    }
}
