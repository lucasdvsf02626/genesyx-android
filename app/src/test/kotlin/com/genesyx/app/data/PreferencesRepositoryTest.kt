package com.genesyx.app.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.genesyx.app.data.local.datastore.GenesyxPreferencesDataStore
import com.genesyx.app.domain.streaks.StreakEngine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * The repository is the only writer of the hydration goal, so the clamp here is what guarantees no
 * reader ever sees a goal of zero — which would divide by zero in the hydration progress bar and be
 * "met" before she drank anything.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PreferencesRepositoryTest {
    @get:Rule val tmp = TemporaryFolder()

    private var fileCounter = 0

    private fun newStore(): GenesyxPreferencesDataStore {
        val ds: DataStore<Preferences> = PreferenceDataStoreFactory.create(
            produceFile = { File(tmp.root, "prefs_${fileCounter++}.preferences_pb") },
        )
        return GenesyxPreferencesDataStore(ds)
    }

    /**
     * The setter is fire-and-forget, and DataStore commits on its own IO dispatcher — which the test
     * scheduler does not control. So wait for the written value to actually arrive rather than read
     * once and race it; reading immediately just returns the default and passes nothing.
     */
    private suspend fun GenesyxPreferencesDataStore.awaitGoalChange(): Int =
        hydrationGoalMl.first { it != StreakEngine.DEFAULT_GOAL_ML }

    @Test
    fun `a goal below the floor is clamped, never persisted as zero`() = runTest {
        val store = newStore()
        val repo = PreferencesRepository(store, backgroundScope)

        repo.setHydrationGoalMl(0)
        advanceUntilIdle()

        assertEquals(StreakEngine.GOAL_RANGE_ML.first, store.awaitGoalChange())
    }

    @Test
    fun `a goal above the ceiling is clamped`() = runTest {
        val store = newStore()
        val repo = PreferencesRepository(store, backgroundScope)

        repo.setHydrationGoalMl(50_000)
        advanceUntilIdle()

        assertEquals(StreakEngine.GOAL_RANGE_ML.last, store.awaitGoalChange())
    }

    @Test
    fun `a goal in range is stored as she set it`() = runTest {
        val store = newStore()
        val repo = PreferencesRepository(store, backgroundScope)

        repo.setHydrationGoalMl(3200)
        advanceUntilIdle()

        assertEquals(3200, store.awaitGoalChange())
    }
}
