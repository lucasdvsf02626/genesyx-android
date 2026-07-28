package com.genesyx.app.ui.track.detail

import app.cash.turbine.test
import com.genesyx.app.data.DailyLogRepository
import com.genesyx.app.domain.model.DailyLog
import com.genesyx.app.util.MainDispatcherRule
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

/**
 * The Sleep editor used to seed its stepper from an unloaded store: `todayMinutes` collapsed both
 * "Room hasn't answered" and "no entry" to 0, so the screen showed "LAST NIGHT 0h 0m" over a real
 * night and the user rebuilt it by hand ("1h 15m"). These pin the state the fix relies on: `loaded`
 * gates the editor, and `todayMinutes` keeps null (no entry) distinct from a saved zero.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SleepDetailViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val logRepo = mockk<DailyLogRepository>()

    private fun vm(
        logs: Map<LocalDate, DailyLog> = emptyMap(),
        loaded: Boolean = true,
    ): SleepDetailViewModel {
        every { logRepo.logByDate } returns MutableStateFlow(logs)
        every { logRepo.loaded } returns MutableStateFlow(loaded)
        return SleepDetailViewModel(logRepo)
    }

    @Test
    fun `no saved entry for last night is null, not zero`() = runTest {
        vm().uiState.test {
            val state = expectMostRecentItem()
            assertTrue(state.loaded)
            assertNull(state.todayMinutes)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a saved entry prefills, and a saved zero stays a zero`() = runTest {
        val today = LocalDate.now()
        vm(logs = mapOf(today to DailyLog(sleepMinutes = 480))).uiState.test {
            assertEquals(480, expectMostRecentItem().todayMinutes)
            cancelAndIgnoreRemainingEvents()
        }
        vm(logs = mapOf(today to DailyLog(sleepMinutes = 0))).uiState.test {
            assertEquals(0, expectMostRecentItem().todayMinutes)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `loaded stays false until the store answers`() = runTest {
        vm(loaded = false).uiState.test {
            assertFalse(expectMostRecentItem().loaded)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a week with a saved night is not the empty state`() = runTest {
        // The walkthrough's "This week: Log a few nights…" beside an 8h entry elsewhere.
        val today = LocalDate.now()
        vm(logs = mapOf(today to DailyLog(sleepMinutes = 480))).uiState.test {
            val state = expectMostRecentItem()
            assertTrue(state.hasData)
            assertEquals(1, state.nightsLogged)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setSleep writes through the shared repository`() {
        val model = vm()
        justRun { logRepo.setSleep(300) }
        model.setSleep(300)
        verify { logRepo.setSleep(300) }
    }
}
