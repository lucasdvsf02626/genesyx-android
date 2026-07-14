package com.genesyx.app.ui.insights

import com.genesyx.app.domain.model.PhReading
import com.genesyx.app.domain.ph.PhStatus
import java.time.LocalDateTime

/**
 * Pure urine-pH insight computation, ported from the web `PhInsightsSection`. Extracted from the
 * ViewModel so it can be unit-tested without coroutines: trend uses the last two readings
 * (threshold 0.1), and the insight/recommendation derive from the 7-day-average status once there
 * are at least two recent readings. Averages are over the 7- and 30-day windows ending at [now].
 */
object PhInsightLogic {

    fun compute(readings: List<PhReading>, now: LocalDateTime = LocalDateTime.now()): PhInsights {
        if (readings.isEmpty()) return PhInsights()

        val sorted = readings.sortedBy { it.recordedAt }
        val latest = sorted.last()
        val previous = sorted.getOrNull(sorted.size - 2)
        val status = PhStatus.classify(latest.phValue)

        fun avgWithin(days: Long): Double? {
            val cutoff = now.minusDays(days)
            val window = sorted.filter { it.recordedAt.isAfter(cutoff) }
            return if (window.isEmpty()) null else window.map { it.phValue }.average()
        }

        val last7 = sorted.filter { it.recordedAt.isAfter(now.minusDays(7)) }
        val last30 = sorted.filter { it.recordedAt.isAfter(now.minusDays(30)) }
        val avg7 = avgWithin(7)
        val avg30 = avgWithin(30)

        val trend = when {
            previous == null -> Trend.FLAT
            latest.phValue - previous.phValue > 0.1 -> Trend.UP
            latest.phValue - previous.phValue < -0.1 -> Trend.DOWN
            else -> Trend.FLAT
        }

        var insight = "Log a few more readings and we'll share gentle observations."
        var recommendation = ""
        if (last7.size >= 2 && avg7 != null) {
            when (PhStatus.classify(avg7)) {
                PhStatus.ACIDIC -> {
                    insight = "Your pH has been trending acidic this week."
                    recommendation = "Try more leafy greens, citrus, and steady hydration to gently shift toward optimal."
                }
                PhStatus.ALKALINE -> {
                    insight = "Your pH has been trending alkaline this week."
                    recommendation = "Balance with whole grains, lean protein, and reduce excess mineral water."
                }
                PhStatus.OPTIMAL -> {
                    insight = "Your pH is sitting comfortably in the optimal range — lovely work."
                    recommendation = "Keep your current hydration and meal rhythm; consistency is the goal."
                }
            }
        }

        return PhInsights(
            hasReadings = true,
            currentValue = latest.phValue,
            currentStatus = status,
            trend = trend,
            avg7 = avg7,
            avg30 = avg30,
            readings30 = last30.size,
            insight = insight,
            recommendation = recommendation,
        )
    }
}
