package com.genesyx.app.ui.insights

import com.genesyx.app.domain.model.DailyLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class IntimacyInsightLogicTest {

    // A Wednesday: the Mon–Sun week is 2026-08-10 .. 2026-08-16.
    private val today = LocalDate.of(2026, 8, 12)
    private val monday = LocalDate.of(2026, 8, 10)

    @Test
    fun `hidden until intimacy has ever been recorded`() {
        val neverLogs = mapOf(monday to DailyLog(mood = null), today to DailyLog(waterMl = 500))
        val result = IntimacyInsightLogic.compute(neverLogs, today)
        assertFalse(result.hasData)
        assertEquals(0, result.daysThisWeek)
    }

    @Test
    fun `a recorded intimacy day shows and counts within the week`() {
        val logs = mapOf(
            monday to DailyLog(sexualActivity = true),               // Mon this week
            monday.plusDays(2) to DailyLog(sexualActivity = true),   // Wed this week
            monday.minusDays(3) to DailyLog(sexualActivity = true),  // last week — not counted in the week
        )
        val result = IntimacyInsightLogic.compute(logs, today)

        assertTrue(result.hasData)
        assertEquals(2, result.daysThisWeek)
        assertEquals(listOf(true, false, true, false, false, false, false), result.perDay)
        assertEquals("Recorded on 2 days this week.", result.insight)
    }

    @Test
    fun `false and null intimacy do not count`() {
        val logs = mapOf(
            monday to DailyLog(sexualActivity = false),
            monday.plusDays(1) to DailyLog(sexualActivity = null),
            monday.plusDays(2) to DailyLog(sexualActivity = true), // the only true, keeps card visible
        )
        val result = IntimacyInsightLogic.compute(logs, today)
        assertTrue(result.hasData)
        assertEquals(1, result.daysThisWeek)
    }
}
