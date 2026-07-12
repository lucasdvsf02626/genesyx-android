package com.genesyx.app.domain.streaks

import com.genesyx.app.domain.model.DailyLog
import com.genesyx.app.domain.model.Mood
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Validates the daily hydration streak, the 5-of-7 weekly streak, and the one-shot milestones. */
class StreakEngineTest {

    // Sunday 2026-06-21 — the last day of its Mon–Sun week, so "this week" is 2026-06-15..21.
    private val today = LocalDate.of(2026, 6, 21)
    private val monday = LocalDate.of(2026, 6, 15)

    private fun water(ml: Int = 250) = DailyLog(waterMl = ml)
    private fun logs(vararg entries: Pair<LocalDate, DailyLog>) = entries.toMap()

    /** [days] consecutive days of water ending on [today]. */
    private fun waterStreak(days: Int): Map<LocalDate, DailyLog> =
        (0 until days).associate { today.minusDays(it.toLong()) to water() }

    /** Water on [count] days of the Mon–Sun week starting at [weekStart]. */
    private fun weekWithDays(weekStart: LocalDate, count: Int): Map<LocalDate, DailyLog> =
        (0 until count).associate { weekStart.plusDays(it.toLong()) to water() }

    // ── daily hydration streak ──

    @Test
    fun `empty history is all zeros`() {
        val s = StreakEngine.compute(emptyMap(), emptySet(), today)
        assertEquals(0, s.dailyHydration)
        assertEquals(0, s.weeklyStreak)
        assertEquals(0, s.daysLoggedThisWeek)
        assertEquals(0, s.bestDailyStreak)
        assertTrue(s.earned.isEmpty())
        assertTrue(s.newMilestones.isEmpty())
    }

    @Test
    fun `daily streak counts consecutive days back from today`() {
        assertEquals(3, StreakEngine.compute(waterStreak(3), emptySet(), today).dailyHydration)
    }

    @Test
    fun `daily streak is zero when today has no water yet`() {
        // Yesterday and before are logged, today is not — same as the shipped behaviour.
        val yesterday = (1..5).associate { today.minusDays(it.toLong()) to water() }
        assertEquals(0, StreakEngine.compute(yesterday, emptySet(), today).dailyHydration)
    }

    @Test
    fun `a gap breaks the daily streak`() {
        val withGap = logs(
            today to water(),
            today.minusDays(1) to water(),
            // no day 2
            today.minusDays(3) to water(),
        )
        assertEquals(2, StreakEngine.compute(withGap, emptySet(), today).dailyHydration)
    }

    @Test
    fun `a day logged without water does not extend the hydration streak`() {
        val moodOnly = logs(today to DailyLog(mood = Mood.GOOD), today.minusDays(1) to water())
        val s = StreakEngine.compute(moodOnly, emptySet(), today)
        assertEquals("water is what the hydration streak counts", 0, s.dailyHydration)
        assertEquals("but the day still counts as activity", 2, s.daysLoggedThisWeek)
    }

    @Test
    fun `best daily streak is the larger of the stored best and the current run`() {
        val s = StreakEngine.compute(waterStreak(3), emptySet(), today, bestSoFar = 9)
        assertEquals(3, s.dailyHydration)
        assertEquals(9, s.bestDailyStreak)

        val beaten = StreakEngine.compute(waterStreak(12), emptySet(), today, bestSoFar = 9)
        assertEquals(12, beaten.bestDailyStreak)
    }

    // ── weekly streak: 5 of 7 days ──

    @Test
    fun `four days in a week is not a complete week`() {
        val s = StreakEngine.compute(weekWithDays(monday, 4), emptySet(), today)
        assertEquals(4, s.daysLoggedThisWeek)
        assertEquals(0, s.weeklyStreak)
    }

    @Test
    fun `five days in a week completes it`() {
        val s = StreakEngine.compute(weekWithDays(monday, 5), emptySet(), today)
        assertEquals(5, s.daysLoggedThisWeek)
        assertEquals(1, s.weeklyStreak)
    }

    @Test
    fun `consecutive complete weeks accumulate`() {
        val threeWeeks = weekWithDays(monday, 5) +
            weekWithDays(monday.minusWeeks(1), 6) +
            weekWithDays(monday.minusWeeks(2), 7)
        assertEquals(3, StreakEngine.compute(threeWeeks, emptySet(), today).weeklyStreak)
    }

    @Test
    fun `an incomplete week resets the streak`() {
        val broken = weekWithDays(monday, 5) +
            weekWithDays(monday.minusWeeks(1), 2) + // too thin — the chain stops here
            weekWithDays(monday.minusWeeks(2), 7)
        assertEquals(1, StreakEngine.compute(broken, emptySet(), today).weeklyStreak)
    }

    @Test
    fun `an in-progress week does not break the streak`() {
        // Tuesday: only two days logged so far this week, but last week was complete.
        val tuesday = monday.plusWeeks(1).plusDays(1)
        val history = weekWithDays(monday, 7) + weekWithDays(monday.plusWeeks(1), 2)
        val s = StreakEngine.compute(history, emptySet(), tuesday)
        assertEquals(2, s.daysLoggedThisWeek)
        assertEquals("last week still counts while this one is under way", 1, s.weeklyStreak)
    }

    @Test
    fun `a ph reading on its own counts as activity for the week`() {
        val phDays = (0 until 5).map { monday.plusDays(it.toLong()) }.toSet()
        val s = StreakEngine.compute(emptyMap(), phDays, today)
        assertEquals(5, s.daysLoggedThisWeek)
        assertEquals(1, s.weeklyStreak)
        assertEquals("pH is not water", 0, s.dailyHydration)
    }

    @Test
    fun `a ph reading and a log on the same day count once`() {
        val s = StreakEngine.compute(weekWithDays(monday, 5), setOf(monday), today)
        assertEquals(5, s.daysLoggedThisWeek)
    }

    @Test
    fun `week activity flags map monday to sunday`() {
        val s = StreakEngine.compute(logs(monday to water(), monday.plusDays(6) to water()), emptySet(), today)
        assertEquals(listOf(true, false, false, false, false, false, true), s.weekActivity)
    }

    // ── milestones ──

    @Test
    fun `day7 and week1 are earned at their thresholds`() {
        val sixDays = StreakEngine.compute(waterStreak(6), emptySet(), today)
        assertFalse(Milestone.DAY_7 in sixDays.earned)

        // 7 days of water back from Sunday also fills the whole Mon–Sun week, so week1 comes too.
        val sevenDays = StreakEngine.compute(waterStreak(7), emptySet(), today)
        assertTrue(Milestone.DAY_7 in sevenDays.earned)
        assertTrue(Milestone.WEEK_1 in sevenDays.earned)
        assertFalse(Milestone.DAY_14 in sevenDays.earned)
    }

    @Test
    fun `an already-celebrated milestone is not new`() {
        val celebrated = setOf(Milestone.DAY_7.id, Milestone.WEEK_1.id)
        val s = StreakEngine.compute(waterStreak(7), emptySet(), today, celebrated = celebrated)
        assertTrue("still earned", Milestone.DAY_7 in s.earned)
        assertTrue("but not fired again", s.newMilestones.isEmpty())
    }

    @Test
    fun `dropping below the threshold un-earns the milestone so it can re-fire`() {
        // She had celebrated day7, then missed today — the streak is 0 and day7 is no longer earned.
        val celebrated = setOf(Milestone.DAY_7.id)
        val lapsed = (1..7).associate { today.minusDays(it.toLong()) to water() }
        val s = StreakEngine.compute(lapsed, emptySet(), today, celebrated = celebrated)
        assertFalse(Milestone.DAY_7 in s.earned)

        // Persisting `earned` clears the flag, so building the streak back up fires it once more.
        val rebuilt = StreakEngine.compute(waterStreak(7), emptySet(), today, celebrated = emptySet())
        assertTrue(Milestone.DAY_7 in rebuilt.newMilestones)
    }

    @Test
    fun `four complete weeks earn the week4 milestone`() {
        val fourWeeks = (0L until 4L).fold(emptyMap<LocalDate, DailyLog>()) { acc, w ->
            acc + weekWithDays(monday.minusWeeks(w), 5)
        }
        val s = StreakEngine.compute(fourWeeks, emptySet(), today)
        assertEquals(4, s.weeklyStreak)
        assertTrue(Milestone.WEEK_4 in s.earned)
        assertTrue(Milestone.WEEK_1 in s.earned)
    }
}
