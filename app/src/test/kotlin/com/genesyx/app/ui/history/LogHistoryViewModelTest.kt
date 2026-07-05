package com.genesyx.app.ui.history

import app.cash.turbine.test
import com.genesyx.app.data.DailyLogRepository
import com.genesyx.app.data.PhRepository
import com.genesyx.app.domain.model.DailyLog
import com.genesyx.app.domain.model.LogDay
import com.genesyx.app.domain.model.PhReading
import com.genesyx.app.util.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class LogHistoryViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val phRepo = mockk<PhRepository>()
    private val logRepo = mockk<DailyLogRepository>()

    private fun vm(
        readings: List<PhReading> = emptyList(),
        logs: Map<LocalDate, DailyLog> = emptyMap(),
    ): LogHistoryViewModel {
        every { phRepo.readings } returns MutableStateFlow(readings)
        every { logRepo.logByDate } returns MutableStateFlow(logs)
        return LogHistoryViewModel(phRepo, logRepo)
    }

    @Test
    fun `groups pH and daily logs by day, newest first`() = runTest {
        val model = vm(
            readings = listOf(
                PhReading(id = "p1", phValue = 6.5, recordedAt = LocalDateTime.of(2026, 7, 5, 9, 0)),
                PhReading(id = "p2", phValue = 7.8, recordedAt = LocalDateTime.of(2026, 7, 5, 20, 0)),
            ),
            logs = mapOf(LocalDate.of(2026, 7, 4) to DailyLog(waterMl = 1000)),
        )
        model.days.test {
            val list = expectMostRecentItem()
            assertEquals(listOf(LocalDate.of(2026, 7, 5), LocalDate.of(2026, 7, 4)), list.map { it.date })
            assertEquals(listOf("p1", "p2"), list[0].phReadings.map { it.id }) // ascending within a day
            assertEquals(1000, list[1].dailyLog?.waterMl)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a pH-only day and a log-only day both appear`() = runTest {
        val model = vm(
            readings = listOf(PhReading(id = "p1", phValue = 6.5, recordedAt = LocalDateTime.of(2026, 7, 6, 8, 0))),
            logs = mapOf(LocalDate.of(2026, 7, 6) to DailyLog(mood = null, waterMl = 500)),
        )
        model.days.test {
            val list = expectMostRecentItem()
            assertEquals(1, list.size) // same day merges into one LogDay
            assertEquals(1, list[0].phReadings.size)
            assertEquals(500, list[0].dailyLog?.waterMl)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `empty daily logs are filtered out`() = runTest {
        val model = vm(logs = mapOf(LocalDate.of(2026, 7, 4) to DailyLog())) // all-default shell
        model.days.test {
            assertEquals(emptyList<LogDay>(), expectMostRecentItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `empty when there are no logs at all`() = runTest {
        vm().days.test {
            assertEquals(emptyList<LogDay>(), expectMostRecentItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
