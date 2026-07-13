package com.genesyx.app.ui.insights

import com.genesyx.app.domain.model.PhReading
import com.genesyx.app.domain.ph.PhStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

/** Validates the pH status thresholds and the pure insight computation (`PhInsightLogic`). */
class PhInsightLogicTest {

    private val now = LocalDateTime.of(2026, 6, 19, 12, 0)
    private fun reading(value: Double, daysAgo: Long) =
        PhReading(phValue = value, recordedAt = now.minusDays(daysAgo))

    // ── PhStatus.classify thresholds (acidic < 6.0, alkaline > 7.5, else optimal) ──

    @Test
    fun `ph status classifies around the 6_0 and 7_5 boundaries`() {
        assertEquals(PhStatus.ACIDIC, PhStatus.classify(5.99))
        assertEquals(PhStatus.OPTIMAL, PhStatus.classify(6.0))
        assertEquals(PhStatus.OPTIMAL, PhStatus.classify(6.8))
        assertEquals(PhStatus.OPTIMAL, PhStatus.classify(7.5))
        assertEquals(PhStatus.ALKALINE, PhStatus.classify(7.51))
    }

    @Test
    fun `ph slider range constants match the web`() {
        assertEquals(4.5, PhStatus.MIN, 0.0)
        assertEquals(9.0, PhStatus.MAX, 0.0)
        assertEquals(0.1, PhStatus.STEP, 0.0)
    }

    // ── PhInsightLogic.compute ──

    @Test
    fun `no readings yields the empty default`() {
        val r = PhInsightLogic.compute(emptyList(), now)
        assertFalse(r.hasReadings)
        assertNull(r.currentValue)
        assertEquals(PhInsights().insight, r.insight)
    }

    @Test
    fun `a single reading is flat with no weekly insight yet`() {
        val r = PhInsightLogic.compute(listOf(reading(6.5, 0)), now)
        assertTrue(r.hasReadings)
        assertEquals(6.5, r.currentValue!!, 1e-9)
        assertEquals(PhStatus.OPTIMAL, r.currentStatus)
        assertEquals(Trend.FLAT, r.trend)
        assertEquals(6.5, r.avg7!!, 1e-9)
        // Fewer than 2 recent readings -> generic insight, no recommendation.
        assertEquals("Log a few more readings and we'll share gentle observations.", r.insight)
        assertEquals("", r.recommendation)
    }

    @Test
    fun `two optimal readings give an optimal insight and rising trend`() {
        val r = PhInsightLogic.compute(listOf(reading(6.4, 2), reading(6.8, 0)), now)
        assertEquals(Trend.UP, r.trend) // 6.8 - 6.4 = 0.4 > 0.1
        assertEquals(6.6, r.avg7!!, 1e-9)
        assertTrue(r.insight.contains("optimal range"))
        assertTrue(r.recommendation.isNotEmpty())
    }

    @Test
    fun `acidic weekly average produces the acidic insight`() {
        val r = PhInsightLogic.compute(listOf(reading(5.5, 1), reading(5.7, 0)), now)
        assertTrue(r.insight.contains("acidic"))
        assertTrue(r.recommendation.contains("leafy greens"))
    }

    @Test
    fun `alkaline weekly average produces the alkaline insight`() {
        val r = PhInsightLogic.compute(listOf(reading(7.8, 1), reading(8.0, 0)), now)
        assertEquals(PhStatus.ALKALINE, r.currentStatus)
        assertTrue(r.insight.contains("alkaline"))
    }

    @Test
    fun `falling and flat trends respect the 0_1 threshold`() {
        val down = PhInsightLogic.compute(listOf(reading(7.0, 1), reading(6.5, 0)), now)
        assertEquals(Trend.DOWN, down.trend) // -0.5

        val flat = PhInsightLogic.compute(listOf(reading(6.50, 1), reading(6.55, 0)), now)
        assertEquals(Trend.FLAT, flat.trend) // 0.05 within threshold
    }

    @Test
    fun `7-day average excludes older readings that the 30-day average includes`() {
        val r = PhInsightLogic.compute(
            listOf(reading(8.0, 20), reading(6.0, 1), reading(6.2, 0)),
            now,
        )
        assertEquals("7-day avg excludes the 20-day-old reading", 6.1, r.avg7!!, 1e-9)
        assertEquals("30-day avg includes all three", (8.0 + 6.0 + 6.2) / 3, r.avg30!!, 1e-9)
    }

    @Test
    fun `readings older than 7 days leave the weekly average null`() {
        val r = PhInsightLogic.compute(listOf(reading(6.5, 10)), now)
        assertTrue(r.hasReadings)
        assertNull("no readings within 7 days", r.avg7)
        assertEquals(6.5, r.avg30!!, 1e-9)
    }

    @Test
    fun `the 30-day reading count says how solid the trend is`() {
        val r = PhInsightLogic.compute(
            listOf(reading(6.5, 40), reading(6.4, 20), reading(6.6, 2), reading(6.5, 0)),
            now,
        )
        assertEquals("the 40-day-old reading is outside the window", 3, r.readings30)
        assertEquals(0, PhInsightLogic.compute(emptyList(), now).readings30)
    }
}
