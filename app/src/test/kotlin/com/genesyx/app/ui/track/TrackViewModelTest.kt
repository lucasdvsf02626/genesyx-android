package com.genesyx.app.ui.track

import app.cash.turbine.test
import com.genesyx.app.data.CycleRepository
import com.genesyx.app.data.DailyLogRepository
import com.genesyx.app.data.PhRepository
import com.genesyx.app.domain.model.CycleSettings
import com.genesyx.app.domain.model.DailyLog
import com.genesyx.app.domain.model.Mood
import com.genesyx.app.domain.model.PhReading
import com.genesyx.app.util.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
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
import java.time.LocalDateTime

/**
 * The calendar's day-detail dialog used to tell every past day "No log yet for this day" because
 * TrackViewModel injected only CycleRepository and structurally could not see the logs. These pin
 * the wiring that fixed it: a day the user logged must come back through [TrackViewModel.logDays].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TrackViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val cycleRepo = mockk<CycleRepository>()
    private val logRepo = mockk<DailyLogRepository>()
    private val phRepo = mockk<PhRepository>()

    private fun vm(
        logs: Map<LocalDate, DailyLog> = emptyMap(),
        readings: List<PhReading> = emptyList(),
    ): TrackViewModel {
        every { cycleRepo.settings } returns MutableStateFlow<CycleSettings?>(null)
        every { logRepo.logByDate } returns MutableStateFlow(logs)
        every { phRepo.readings } returns MutableStateFlow(readings)
        return TrackViewModel(cycleRepo, logRepo, phRepo)
    }

    @Test
    fun `a past day the user logged is retrievable for that date`() = runTest {
        val logged = LocalDate.of(2026, 7, 4)
        val model = vm(
            logs = mapOf(
                logged to DailyLog(
                    mood = Mood.GOOD,
                    symptoms = setOf("Cramps"),
                    supplements = setOf("Iron"),
                    waterMl = 1500,
                ),
            ),
        )

        model.logDays.test {
            val day = expectMostRecentItem().getValue(logged)
            assertTrue(day.hasDailyContent)          // the dialog's "is there a log" gate
            assertEquals(Mood.GOOD, day.dailyLog?.mood)
            assertEquals(setOf("Cramps"), day.dailyLog?.symptoms)
            assertEquals(1500, day.dailyLog?.waterMl)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a day with only pH readings still counts as logged`() = runTest {
        val logged = LocalDate.of(2026, 7, 6)
        val model = vm(
            readings = listOf(
                PhReading(id = "p2", phValue = 7.0, recordedAt = LocalDateTime.of(2026, 7, 6, 20, 0)),
                PhReading(id = "p1", phValue = 6.5, recordedAt = LocalDateTime.of(2026, 7, 6, 8, 0)),
            ),
        )

        model.logDays.test {
            val day = expectMostRecentItem().getValue(logged)
            assertFalse(day.isEmpty)                             // dialog must not say "nothing logged"
            assertFalse(day.hasDailyContent)                     // ...but there is no daily log
            assertEquals(listOf("p1", "p2"), day.phReadings.map { it.id }) // ascending by time
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a day the user never logged has no entry`() = runTest {
        val model = vm(logs = mapOf(LocalDate.of(2026, 7, 4) to DailyLog(waterMl = 800)))

        model.logDays.test {
            // Only then may the dialog say "Nothing logged on this day."
            assertNull(expectMostRecentItem()[LocalDate.of(2026, 7, 1)])
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `an empty log shell does not read as content`() = runTest {
        val blank = LocalDate.of(2026, 7, 4)
        val model = vm(logs = mapOf(blank to DailyLog())) // all-default row, nothing entered

        model.logDays.test {
            val day = expectMostRecentItem().getValue(blank)
            assertTrue(day.isEmpty)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
