package com.genesyx.app.ui.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.genesyx.app.data.PhRepository
import com.genesyx.app.domain.model.PhReading
import com.genesyx.app.domain.ph.PhStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDateTime
import javax.inject.Inject

enum class Trend { UP, DOWN, FLAT }

data class PhInsights(
    val hasReadings: Boolean = false,
    val currentValue: Double? = null,
    val currentStatus: PhStatus? = null,
    val trend: Trend = Trend.FLAT,
    val avg7: Double? = null,
    val avg30: Double? = null,
    val insight: String = "Log a few pH readings to see personalised insights here.",
    val recommendation: String = "",
)

@HiltViewModel
class InsightsViewModel @Inject constructor(
    phRepository: PhRepository,
) : ViewModel() {

    val phInsights: StateFlow<PhInsights> =
        phRepository.readings
            .map { readings -> buildInsights(readings) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = PhInsights(),
            )

    private fun buildInsights(readings: List<PhReading>): PhInsights {
        if (readings.isEmpty()) return PhInsights()

        val now = LocalDateTime.now()
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
        val avg7 = avgWithin(7)
        val avg30 = avgWithin(30)

        // Trend vs the previous reading (threshold 0.1), matching PhInsightsSection.tsx.
        val trend = when {
            previous == null -> Trend.FLAT
            latest.phValue - previous.phValue > 0.1 -> Trend.UP
            latest.phValue - previous.phValue < -0.1 -> Trend.DOWN
            else -> Trend.FLAT
        }

        // Insight is derived from the 7-day average status, only once there are 2+ recent readings.
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
            insight = insight,
            recommendation = recommendation,
        )
    }
}
