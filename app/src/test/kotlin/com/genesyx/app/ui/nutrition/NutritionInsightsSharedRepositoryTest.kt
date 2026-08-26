package com.genesyx.app.ui.nutrition

import com.genesyx.app.core.log.Logger
import com.genesyx.app.core.result.DataResult
import com.genesyx.app.data.ConsentRepository
import com.genesyx.app.data.CycleRepository
import com.genesyx.app.data.DailyLogRepository
import com.genesyx.app.data.FakeDailyLogDao
import com.genesyx.app.data.GenesyxProductRepository
import com.genesyx.app.data.MealLogRepository
import com.genesyx.app.data.PhRepository
import com.genesyx.app.data.PreferencesRepository
import com.genesyx.app.data.SessionRepository
import com.genesyx.app.data.StreakRepository
import com.genesyx.app.data.SupplementReminderRepository
import com.genesyx.app.data.UserSupplementRepository
import com.genesyx.app.data.remote.DailyLogRemoteDataSource
import com.genesyx.app.data.sync.DailyLogSyncScheduler
import com.genesyx.app.domain.consent.HealthDataCollectionGate
import com.genesyx.app.domain.hydration.HydrationUnit
import com.genesyx.app.domain.model.CycleSettings
import com.genesyx.app.domain.model.PhReading
import com.genesyx.app.domain.model.UserSupplement
import com.genesyx.app.domain.streaks.StreakState
import com.genesyx.app.ui.insights.InsightsViewModel
import com.genesyx.app.ui.track.detail.NutritionDetailViewModel
import com.genesyx.app.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

/**
 * SFM-28b's acceptance, at the view-model layer: the Insights "Nutrition consistency" card and
 * the Track → Nutrition summary derive their numbers from the **same** [DailyLogRepository] the
 * Nutrition tab's chips write to. One real repository over one fake table; three view-models
 * reading it. A toggle on the Nutrition view-model must show up on both of the others with no
 * second query path in between.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NutritionInsightsSharedRepositoryTest {

    @get:Rule val mainDispatcher = MainDispatcherRule()

    private val today: LocalDate = LocalDate.now()
    private val custom = MutableStateFlow<List<UserSupplement>>(emptyList())

    private fun session(): SessionRepository = mockk<SessionRepository>().also {
        every { it.userId } returns MutableStateFlow<String?>("user-a")
        every { it.currentUserId() } returns "user-a"
    }

    private fun repository(scope: CoroutineScope, gate: HealthDataCollectionGate): DailyLogRepository {
        val remote = mockk<DailyLogRemoteDataSource>().also {
            coEvery { it.upsertLog(any(), any(), any()) } returns DataResult.Success(Unit)
        }
        return DailyLogRepository(
            FakeDailyLogDao(), remote, session(), mockk<DailyLogSyncScheduler>(relaxed = true),
            mockk<Logger>(relaxed = true), scope, gate,
        )
    }

    private val preferences = mockk<PreferencesRepository>(relaxed = true).also {
        every { it.hydrationGoalMl } returns MutableStateFlow(2400)
        every { it.hydrationUnit } returns MutableStateFlow(HydrationUnit.ML)
        every { it.hydrationGlassMl } returns MutableStateFlow(200)
    }
    private val cycles = mockk<CycleRepository>().also {
        every { it.settings } returns MutableStateFlow<CycleSettings?>(null)
    }
    private val streaks = mockk<StreakRepository>().also {
        every { it.state } returns MutableStateFlow(StreakState())
    }
    private val userSupplements = mockk<UserSupplementRepository>(relaxed = true).also {
        every { it.supplements } returns custom
    }
    private val reminders = mockk<SupplementReminderRepository>(relaxed = true).also {
        every { it.reminders } returns MutableStateFlow(emptyMap())
    }
    private val meals = mockk<MealLogRepository>(relaxed = true).also {
        every { it.mealsForDate(any()) } returns flowOf(emptyList())
    }
    private val products = mockk<GenesyxProductRepository>().also {
        coEvery { it.fetchCatalogue() } returns emptyList()
    }
    private val consent = mockk<ConsentRepository>().also {
        every { it.isActive } returns MutableStateFlow(true)
    }
    private val ph = mockk<PhRepository>().also {
        every { it.readings } returns MutableStateFlow<List<PhReading>>(emptyList())
    }

    private fun nutritionViewModel(repo: DailyLogRepository) = NutritionViewModel(
        cycles, repo, preferences, userSupplements, reminders, meals, products, streaks, consent,
    )

    private fun insightsViewModel(repo: DailyLogRepository) = InsightsViewModel(
        ph, repo, cycles, streaks, preferences, userSupplements,
    )

    @Test
    fun `a chip toggled on Nutrition is counted by Insights and the Track summary from the same repository`() = runTest(mainDispatcher.dispatcher.scheduler) {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        val repo = repository(scope, HealthDataCollectionGate { true })
        val nutrition = nutritionViewModel(repo)
        val insights = insightsViewModel(repo)
        val tracker = NutritionDetailViewModel(repo)
        // WhileSubscribed flows need a collector to be live, exactly as a visible screen is.
        backgroundScope.launch { nutrition.uiState.collect {} }
        backgroundScope.launch { insights.supplementInsights.collect {} }
        backgroundScope.launch { tracker.uiState.collect {} }
        runCurrent()

        val folate = nutrition.planEntries.value.first { it.display == "Folate" }
        nutrition.toggleSupplement(folate)
        advanceUntilIdle()

        assertEquals(setOf("Folic acid"), nutrition.uiState.value.loggedToday)
        val card = insights.supplementInsights.value
        assertTrue("the card leaves its empty state the moment something is logged this week", card.hasData)
        assertEquals(1, card.todayTaken)
        assertEquals(4, card.planSize)
        assertEquals(listOf(true, false, false, false), card.todayItems.map { it.logged })
        assertEquals(1, card.daysLogged)
        assertEquals(listOf("Folate"), tracker.uiState.value.todaySupplements)
        assertFalse(tracker.uiState.value.supplementWeekEmpty)

        // Un-log the only item: every reader drops back to zero — the clear was really written.
        nutrition.toggleSupplement(folate)
        advanceUntilIdle()
        assertEquals(emptySet<String>(), nutrition.uiState.value.loggedToday)
        assertEquals(0, insights.supplementInsights.value.todayTaken)
        assertFalse(insights.supplementInsights.value.hasData)
        assertTrue(tracker.uiState.value.todaySupplements.isEmpty())
        scope.cancel()
    }

    @Test
    fun `her own supplement joins the chips and the denominator on both screens`() = runTest(mainDispatcher.dispatcher.scheduler) {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        val repo = repository(scope, HealthDataCollectionGate { true })
        val nutrition = nutritionViewModel(repo)
        val insights = insightsViewModel(repo)
        backgroundScope.launch { nutrition.uiState.collect {} }
        backgroundScope.launch { insights.supplementInsights.collect {} }
        runCurrent()

        custom.value = listOf(UserSupplement(name = "Magnesium", dose = "300 mg"))
        runCurrent()

        assertEquals(listOf("Folate", "Omega-3", "Vitamin D", "Zinc", "Magnesium"), nutrition.planEntries.value.map { it.display })
        nutrition.toggleSupplement(nutrition.planEntries.value.last())
        advanceUntilIdle()

        assertEquals(5, insights.supplementInsights.value.planSize)
        assertEquals(1, insights.supplementInsights.value.todayTaken)
        assertTrue(insights.supplementInsights.value.todayItems.any { it.name == "Magnesium" && it.logged })
        scope.cancel()
    }

    @Test
    fun `a refused toggle is reported to the screen and changes nothing anywhere`() = runTest(mainDispatcher.dispatcher.scheduler) {
        val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
        val repo = repository(scope, HealthDataCollectionGate { false })
        val nutrition = nutritionViewModel(repo)
        val insights = insightsViewModel(repo)
        backgroundScope.launch { nutrition.uiState.collect {} }
        backgroundScope.launch { insights.supplementInsights.collect {} }
        val events = mutableListOf<SupplementSaveEvent>()
        backgroundScope.launch { nutrition.supplementEvents.collect { events += it } }
        runCurrent()

        nutrition.toggleSupplement(nutrition.planEntries.value.first())
        advanceUntilIdle()

        assertEquals(1, events.size)
        assertTrue(events.single() is SupplementSaveEvent.Refused)
        assertTrue(events.single().text.contains("consent", ignoreCase = true))
        assertEquals(emptySet<String>(), nutrition.uiState.value.loggedToday)
        assertEquals(0, insights.supplementInsights.value.todayTaken)
        scope.cancel()
    }
}
