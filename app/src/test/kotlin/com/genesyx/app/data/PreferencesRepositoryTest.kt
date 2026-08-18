package com.genesyx.app.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.genesyx.app.data.local.datastore.GenesyxPreferencesDataStore
import com.genesyx.app.domain.hydration.HydrationFormat
import com.genesyx.app.domain.streaks.StreakEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
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

    /**
     * Scopes owned by this test, one per store, so they can be shut down deterministically.
     *
     * Without an explicit `scope`, [PreferenceDataStoreFactory] runs the store on coroutines this
     * test does not own and cannot stop. Those outlive the test method, but [TemporaryFolder]
     * deletes its directory the moment the method returns — so a still-running store reads a path
     * that no longer exists and dies with `FileNotFoundException`. Whether that lands inside the
     * test or after it is pure timing, which is why this class failed intermittently rather than
     * consistently. Owning the scope and joining its cancellation in [closeStores] removes the race
     * instead of hiding it: JUnit runs `@After` before the `@Rule` tears down, so every store is
     * fully stopped before the directory goes.
     */
    private val storeScopes = mutableListOf<CoroutineScope>()

    private fun newStore(): GenesyxPreferencesDataStore {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        storeScopes += scope
        val ds: DataStore<Preferences> = PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { File(tmp.root, "prefs_${fileCounter++}.preferences_pb") },
        )
        return GenesyxPreferencesDataStore(ds)
    }

    @After
    fun closeStores() = runBlocking {
        // cancelAndJoin, not cancel: cancellation is asynchronous, and returning before the store's
        // coroutines have actually finished would leave exactly the race this is here to close.
        storeScopes.forEach { it.coroutineContext.job.cancelAndJoin() }
        storeScopes.clear()
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

    // Same single-writer clamp discipline for the glass size — a zero glass would make the
    // quick-add buttons silent no-ops with nothing on screen to explain why.

    private suspend fun GenesyxPreferencesDataStore.awaitGlassChange(): Int =
        hydrationGlassMl.first { it != HydrationFormat.DEFAULT_GLASS_ML }

    @Test
    fun `a glass below the floor is clamped`() = runTest {
        val store = newStore()
        val repo = PreferencesRepository(store, backgroundScope)

        repo.setHydrationGlassMl(0)
        advanceUntilIdle()

        assertEquals(HydrationFormat.GLASS_RANGE_ML.first, store.awaitGlassChange())
    }

    @Test
    fun `a glass above the ceiling is clamped`() = runTest {
        val store = newStore()
        val repo = PreferencesRepository(store, backgroundScope)

        repo.setHydrationGlassMl(5_000)
        advanceUntilIdle()

        assertEquals(HydrationFormat.GLASS_RANGE_ML.last, store.awaitGlassChange())
    }

    @Test
    fun `a glass in range is stored as she set it`() = runTest {
        val store = newStore()
        val repo = PreferencesRepository(store, backgroundScope)

        repo.setHydrationGlassMl(330)
        advanceUntilIdle()

        assertEquals(330, store.awaitGlassChange())
    }
}
