package com.genesyx.app.domain.hydration

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class HydrationCoachTest {

    private val goal = 2400

    @Test
    fun `reaching the goal is celebrated whatever the hour`() {
        val r = HydrationCoach.coach(currentMl = 2400, goalMl = goal, now = LocalTime.of(10, 0))
        assertEquals(HydrationPace.REACHED, r.pace)
    }

    @Test
    fun `before the waking window there is no expectation`() {
        // 6am, nothing drunk yet — a fresh morning is not "behind".
        val r = HydrationCoach.coach(currentMl = 0, goalMl = goal, now = LocalTime.of(6, 0))
        assertEquals(HydrationPace.NOT_STARTED, r.pace)
    }

    @Test
    fun `expected pace rises linearly across the day`() {
        // Window 08:00..22:00 (14h). At 15:00, 7h in -> 50%.
        assertEquals(0.5, HydrationCoach.expectedFraction(LocalTime.of(15, 0)), 0.01)
        assertEquals(0.0, HydrationCoach.expectedFraction(LocalTime.of(7, 0)), 0.01)
        assertEquals(1.0, HydrationCoach.expectedFraction(LocalTime.of(23, 0)), 0.01)
    }

    @Test
    fun `keeping up with the day reads as on track`() {
        // 15:00 expects ~50% = 1200ml; drinking 1200 is on track.
        val r = HydrationCoach.coach(currentMl = 1200, goalMl = goal, now = LocalTime.of(15, 0))
        assertEquals(HydrationPace.ON_TRACK, r.pace)
    }

    @Test
    fun `well past the day's pace reads as ahead`() {
        // 15:00 expects ~50%; 2000ml is ~83% -> ahead.
        val r = HydrationCoach.coach(currentMl = 2000, goalMl = goal, now = LocalTime.of(15, 0))
        assertEquals(HydrationPace.AHEAD, r.pace)
    }

    @Test
    fun `falling behind the pace is named gently, never scolded`() {
        // 20:00 expects ~86% = ~2057ml; only 500ml -> behind.
        val r = HydrationCoach.coach(currentMl = 500, goalMl = goal, now = LocalTime.of(20, 0))
        assertEquals(HydrationPace.BEHIND, r.pace)
        listOf("should", "fail", "bad", "poor", "behind on your goal", "only").forEach {
            assertFalse("copy must not scold: '$it' in \"${r.message}\"", r.message.lowercase().contains(it))
        }
    }

    @Test
    fun `the message is framed by part of day`() {
        assertTrue(HydrationCoach.coach(300, goal, LocalTime.of(9, 0)).message.startsWith("This morning"))
        assertTrue(HydrationCoach.coach(300, goal, LocalTime.of(14, 0)).message.startsWith("This afternoon"))
        assertTrue(HydrationCoach.coach(300, goal, LocalTime.of(19, 0)).message.startsWith("This evening"))
    }

    @Test
    fun `a zero goal cannot divide by zero`() {
        // PreferencesRepository clamps the real goal, but the coach must be safe regardless: the goal
        // floors to 1, so this simply computes a pace instead of crashing.
        val r = HydrationCoach.coach(currentMl = 0, goalMl = 0, now = LocalTime.of(12, 0))
        assertEquals(HydrationPace.BEHIND, r.pace)
    }
}
