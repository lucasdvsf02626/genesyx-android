package com.genesyx.app.ui.insights

import com.genesyx.app.domain.streaks.StreakState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The Consistency card's copy — it has to stay encouraging whatever the numbers do. */
class ConsistencyInsightLogicTest {

    @Test
    fun `an empty week invites a first log instead of reporting a zero`() {
        val r = ConsistencyInsightLogic.compute(StreakState())
        assertEquals(0, r.daysLoggedThisWeek)
        assertTrue(r.insight.contains("starts counting"))
        assertFalse("no zero-shaming", r.insight.contains("0 of 7"))
    }

    @Test
    fun `a partial week states the count and the threshold`() {
        val r = ConsistencyInsightLogic.compute(StreakState(daysLoggedThisWeek = 3))
        assertEquals("You've logged 3 of 7 days this week. Five days makes the week count.", r.insight)
    }

    @Test
    fun `one steady week is singular`() {
        val r = ConsistencyInsightLogic.compute(StreakState(daysLoggedThisWeek = 5, weeklyStreak = 1))
        assertEquals("You've logged 5 of 7 days this week — one steady week behind you.", r.insight)
    }

    @Test
    fun `several steady weeks are counted`() {
        val r = ConsistencyInsightLogic.compute(StreakState(daysLoggedThisWeek = 6, weeklyStreak = 4))
        assertEquals("You've logged 6 of 7 days this week — 4 steady weeks behind you.", r.insight)
    }

    @Test
    fun `the streak numbers pass through from the engine`() {
        val r = ConsistencyInsightLogic.compute(
            StreakState(
                dailyHydration = 9,
                weeklyStreak = 2,
                daysLoggedThisWeek = 5,
                weekActivity = listOf(true, true, true, true, true, false, false),
                bestDailyStreak = 21,
            ),
        )
        assertEquals(9, r.dailyStreak)
        assertEquals(2, r.weeklyStreak)
        assertEquals(21, r.bestDailyStreak)
        assertEquals(listOf(true, true, true, true, true, false, false), r.weekActivity)
    }

    @Test
    fun `the copy never mentions a broken or lost streak`() {
        val banned = listOf("broke", "broken", "lost", "failed", "missed")
        val states = listOf(
            StreakState(),
            StreakState(daysLoggedThisWeek = 1),
            StreakState(daysLoggedThisWeek = 4),
            StreakState(daysLoggedThisWeek = 7, weeklyStreak = 3),
        )
        states.forEach { s ->
            val insight = ConsistencyInsightLogic.compute(s).insight.lowercase()
            banned.forEach { word ->
                assertFalse("'$word' in: $insight", insight.contains(word))
            }
        }
    }
}
