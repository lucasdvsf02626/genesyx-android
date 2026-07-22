package com.genesyx.app.ui.insights

import com.genesyx.app.domain.model.PhMeasurement
import com.genesyx.app.domain.model.PhReading
import com.genesyx.app.domain.ph.PhCopy
import com.genesyx.app.domain.ph.PhStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

/**
 * Validates the vaginal-pH status thresholds and the pure insight computation (`PhInsightLogic`).
 * Figures are PROVISIONAL — pending clinical sign-off (see PhStatus).
 */
class PhInsightLogicTest {

    private val now = LocalDateTime.of(2026, 6, 19, 12, 0)
    private fun reading(value: Double, daysAgo: Long) =
        PhReading(phValue = value, recordedAt = now.minusDays(daysAgo))
    private fun urineReading(value: Double, daysAgo: Long) =
        PhReading(phValue = value, recordedAt = now.minusDays(daysAgo), measurementType = PhMeasurement.URINE)

    // ── PhStatus.classify: two-band (Healthy <= 4.5, Elevated > 4.5) ──

    @Test
    fun `ph status classifies around the 4_5 healthy threshold`() {
        assertEquals(PhStatus.HEALTHY, PhStatus.classify(3.8))
        assertEquals(PhStatus.HEALTHY, PhStatus.classify(4.2))
        assertEquals(PhStatus.HEALTHY, PhStatus.classify(4.5))
        assertEquals(PhStatus.ELEVATED, PhStatus.classify(4.51))
        assertEquals(PhStatus.ELEVATED, PhStatus.classify(5.5))
    }

    @Test
    fun `ph range constants are the provisional vaginal figures`() {
        assertEquals(3.5, PhStatus.MIN, 0.0)
        assertEquals(7.0, PhStatus.MAX, 0.0)
        assertEquals(0.1, PhStatus.STEP, 0.0)
        assertEquals(3.8, PhStatus.HEALTHY_MIN, 0.0)
        assertEquals(4.5, PhStatus.HEALTHY_MAX, 0.0)
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
    fun `legacy urine readings are excluded and do not drive insights`() {
        // Only urine readings -> treated as no vaginal data at all.
        val urineOnly = PhInsightLogic.compute(listOf(urineReading(6.5, 1), urineReading(6.8, 0)), now)
        assertFalse(urineOnly.hasReadings)

        // A vaginal reading alongside a (more recent) urine one: only the vaginal reading counts.
        val mixed = PhInsightLogic.compute(listOf(reading(4.2, 2), urineReading(6.8, 0)), now)
        assertTrue(mixed.hasReadings)
        assertEquals(4.2, mixed.currentValue!!, 1e-9)
        assertEquals(PhStatus.HEALTHY, mixed.currentStatus)
    }

    @Test
    fun `a single reading is flat with no weekly insight yet`() {
        val r = PhInsightLogic.compute(listOf(reading(4.2, 0)), now)
        assertTrue(r.hasReadings)
        assertEquals(4.2, r.currentValue!!, 1e-9)
        assertEquals(PhStatus.HEALTHY, r.currentStatus)
        assertEquals(Trend.FLAT, r.trend)
        assertEquals(4.2, r.avg7!!, 1e-9)
        // Fewer than 2 recent readings -> generic insight, no recommendation.
        assertEquals(PhCopy.INSIGHT_DEFAULT, r.insight)
        assertEquals("", r.recommendation)
    }

    @Test
    fun `two healthy readings give a healthy insight and rising trend`() {
        val r = PhInsightLogic.compute(listOf(reading(4.0, 2), reading(4.2, 0)), now)
        assertEquals(Trend.UP, r.trend) // 4.2 - 4.0 = 0.2 > 0.1
        assertEquals(4.1, r.avg7!!, 1e-9)
        assertEquals(PhCopy.INSIGHT_HEALTHY, r.insight)
        assertEquals("", r.recommendation) // healthy state gives no recommendation
    }

    @Test
    fun `elevated weekly average produces the elevated insight and signpost`() {
        val r = PhInsightLogic.compute(listOf(reading(4.8, 1), reading(5.0, 0)), now)
        assertEquals(PhStatus.ELEVATED, r.currentStatus)
        assertEquals(PhCopy.INSIGHT_ELEVATED, r.insight)
        assertEquals(PhCopy.RECOMMENDATION_ELEVATED, r.recommendation)
        assertTrue(r.recommendation.isNotEmpty())
    }

    @Test
    fun `falling and flat trends respect the 0_1 threshold`() {
        val down = PhInsightLogic.compute(listOf(reading(4.6, 1), reading(4.2, 0)), now)
        assertEquals(Trend.DOWN, down.trend) // -0.4

        val flat = PhInsightLogic.compute(listOf(reading(4.20, 1), reading(4.25, 0)), now)
        assertEquals(Trend.FLAT, flat.trend) // 0.05 within threshold
    }

    @Test
    fun `7-day average excludes older readings that the 30-day average includes`() {
        val r = PhInsightLogic.compute(
            listOf(reading(5.0, 20), reading(4.0, 1), reading(4.2, 0)),
            now,
        )
        assertEquals("7-day avg excludes the 20-day-old reading", 4.1, r.avg7!!, 1e-9)
        assertEquals("30-day avg includes all three", (5.0 + 4.0 + 4.2) / 3, r.avg30!!, 1e-9)
    }

    @Test
    fun `readings older than 7 days leave the weekly average null`() {
        val r = PhInsightLogic.compute(listOf(reading(4.2, 10)), now)
        assertTrue(r.hasReadings)
        assertNull("no readings within 7 days", r.avg7)
        assertEquals(4.2, r.avg30!!, 1e-9)
    }

    @Test
    fun `the 30-day reading count says how solid the trend is`() {
        val r = PhInsightLogic.compute(
            listOf(reading(4.2, 40), reading(4.1, 20), reading(4.3, 2), reading(4.2, 0)),
            now,
        )
        assertEquals("the 40-day-old reading is outside the window", 3, r.readings30)
        assertEquals(0, PhInsightLogic.compute(emptyList(), now).readings30)
    }
}
