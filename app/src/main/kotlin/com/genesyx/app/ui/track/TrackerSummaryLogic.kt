package com.genesyx.app.ui.track

import com.genesyx.app.domain.content.phaseLabel
import com.genesyx.app.domain.cycle.CycleEngine
import com.genesyx.app.domain.model.CycleSettings
import com.genesyx.app.domain.model.DailyLog
import com.genesyx.app.domain.model.PhReading
import com.genesyx.app.domain.model.Supplement
import com.genesyx.app.domain.ph.PhStatus
import com.genesyx.app.ui.insights.SleepInsightLogic
import java.time.LocalDate

/** One row's worth of honest summary for the "Your Trackers" list. */
data class TrackerSummary(
    val value: String,
    /** Seven booleans, oldest→newest (last 7 days incl. today): true where that signal has data. */
    val spark: List<Boolean> = emptyList(),
    val hasData: Boolean = false,
)

data class TrackerSummaries(
    val cycle: TrackerSummary,
    val hydration: TrackerSummary,
    val ph: TrackerSummary,
    val sleep: TrackerSummary,
    val symptoms: TrackerSummary,
    val nutrition: TrackerSummary,
)

/** Neutral placeholder for a StateFlow's initial value, before real data has loaded. */
fun emptyTrackerSummaries(): TrackerSummaries {
    val blank = TrackerSummary("", hasData = false)
    return TrackerSummaries(blank, blank, blank, blank, blank, blank)
}

/**
 * Pure real-data summaries for Track's "Your Trackers" rows. Every value comes from stored logs,
 * cycle settings, or pH readings — never a fabricated zero or healthy status. A signal with no data
 * returns an honest invitation and `hasData = false`, and its spark row is all-false.
 */
object TrackerSummaryLogic {

    fun compute(
        logsByDate: Map<LocalDate, DailyLog>,
        readings: List<PhReading>,
        settings: CycleSettings?,
        goalMl: Int,
        plan: List<Supplement> = Supplement.defaultPlan,
        today: LocalDate = LocalDate.now(),
    ): TrackerSummaries {
        val week = (6L downTo 0L).map { today.minusDays(it) } // oldest → newest, incl. today
        fun log(d: LocalDate) = logsByDate[d]

        return TrackerSummaries(
            cycle = cycle(settings, today),
            hydration = hydration(logsByDate, week, goalMl, today),
            ph = ph(readings, week),
            sleep = sleep(logsByDate, week),
            symptoms = symptoms(logsByDate, week),
            nutrition = nutrition(logsByDate, week, plan, today),
        )
    }

    private fun cycle(settings: CycleSettings?, today: LocalDate): TrackerSummary {
        if (settings == null) {
            return TrackerSummary("Set up your cycle to see your phases", hasData = false)
        }
        val info = CycleEngine.getCyclePhase(settings, today)
        return TrackerSummary("Day ${info.dayOfCycle} · ${phaseLabel.getValue(info.phase)}", hasData = true)
    }

    private fun hydration(
        logs: Map<LocalDate, DailyLog>,
        week: List<LocalDate>,
        goalMl: Int,
        today: LocalDate,
    ): TrackerSummary {
        val spark = week.map { (logs[it]?.waterMl ?: 0) > 0 }
        val todayMl = logs[today]?.waterMl ?: 0
        return if (todayMl > 0) {
            TrackerSummary("${litres(todayMl)} of ${litres(goalMl)} today", spark, hasData = true)
        } else {
            TrackerSummary("No water logged today", spark, hasData = false)
        }
    }

    private fun ph(readings: List<PhReading>, week: List<LocalDate>): TrackerSummary {
        val byDay = readings.groupBy { it.recordedAt.toLocalDate() }
        val spark = week.map { byDay.containsKey(it) }
        val latest = readings.maxByOrNull { it.recordedAt }
        return if (latest != null) {
            val status = PhStatus.classify(latest.phValue)
            TrackerSummary("Last reading %.1f · ${status.label}".format(latest.phValue), spark, hasData = true)
        } else {
            TrackerSummary("No readings yet — log your first", spark, hasData = false)
        }
    }

    private fun sleep(logs: Map<LocalDate, DailyLog>, week: List<LocalDate>): TrackerSummary {
        val spark = week.map { (logs[it]?.sleepMinutes ?: 0) > 0 }
        val latest = week.lastOrNull { (logs[it]?.sleepMinutes ?: 0) > 0 }
        return if (latest != null) {
            val minutes = logs[latest]!!.sleepMinutes!!
            TrackerSummary("${SleepInsightLogic.formatDuration(minutes)} most recent night", spark, hasData = true)
        } else {
            TrackerSummary("No sleep logged this week", spark, hasData = false)
        }
    }

    private fun symptoms(logs: Map<LocalDate, DailyLog>, week: List<LocalDate>): TrackerSummary {
        val spark = week.map { logs[it]?.symptoms?.isNotEmpty() == true }
        val total = week.sumOf { logs[it]?.symptoms?.size ?: 0 }
        return if (total > 0) {
            TrackerSummary("$total ${if (total == 1) "symptom" else "symptoms"} logged this week", spark, hasData = true)
        } else {
            TrackerSummary("No symptoms logged this week", spark, hasData = false)
        }
    }

    private fun nutrition(
        logs: Map<LocalDate, DailyLog>,
        week: List<LocalDate>,
        plan: List<Supplement>,
        today: LocalDate,
    ): TrackerSummary {
        fun takenOn(d: LocalDate): Int {
            val logged = logs[d]?.supplements.orEmpty().mapNotNull(Supplement::fromWire)
            return plan.count { it in logged }
        }
        val spark = week.map { takenOn(it) > 0 }
        val todayTaken = takenOn(today)
        return if (todayTaken > 0) {
            TrackerSummary("$todayTaken of ${plan.size} supplements today", spark, hasData = true)
        } else {
            TrackerSummary("No supplements logged today", spark, hasData = false)
        }
    }

    /** "1.6 L" — one decimal, matching the hydration surfaces elsewhere. */
    private fun litres(ml: Int): String = "%.1f L".format(ml / 1000f)
}
