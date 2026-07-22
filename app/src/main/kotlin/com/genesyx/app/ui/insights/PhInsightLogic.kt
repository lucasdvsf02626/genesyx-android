package com.genesyx.app.ui.insights

import com.genesyx.app.domain.model.PhMeasurement
import com.genesyx.app.domain.model.PhReading
import com.genesyx.app.domain.ph.PhCopy
import com.genesyx.app.domain.ph.PhStatus
import java.time.LocalDateTime

/**
 * Pure vaginal-pH insight computation. Extracted from the ViewModel so it can be unit-tested without
 * coroutines: trend uses the last two readings (threshold 0.1), and the insight/recommendation derive
 * from the 7-day-average status once there are at least two recent readings. Averages are over the 7-
 * and 30-day windows ending at [now].
 *
 * Legacy urine readings are excluded from every computation — they are on a different scale, so
 * classifying them as Healthy/Elevated would be wrong. Only vaginal readings drive insights.
 */
object PhInsightLogic {

    fun compute(readings: List<PhReading>, now: LocalDateTime = LocalDateTime.now()): PhInsights {
        val vaginal = readings.filter { it.measurementType == PhMeasurement.VAGINAL }
        if (vaginal.isEmpty()) return PhInsights()

        val sorted = vaginal.sortedBy { it.recordedAt }
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

        var insight = PhCopy.INSIGHT_DEFAULT
        var recommendation = ""
        if (last7.size >= 2 && avg7 != null) {
            when (PhStatus.classify(avg7)) {
                PhStatus.HEALTHY -> {
                    insight = PhCopy.INSIGHT_HEALTHY
                    recommendation = ""
                }
                PhStatus.ELEVATED -> {
                    insight = PhCopy.INSIGHT_ELEVATED
                    recommendation = PhCopy.RECOMMENDATION_ELEVATED
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
