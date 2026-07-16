package com.genesyx.app.ui.insights

import com.genesyx.app.domain.model.DailyLog
import com.genesyx.app.domain.model.EnergyLevel
import com.genesyx.app.domain.model.Mood
import com.genesyx.app.domain.model.Supplement
import com.genesyx.app.domain.model.isMeaningful
import com.genesyx.app.domain.time.WeekBuckets
import java.time.LocalDate

/**
 * The week in review: the current Mon–Sun week set against the one before it. It reads the same
 * [WeekBuckets] the other cards do, so "this week" means the same seven days everywhere.
 *
 * It invents no comparison it cannot stand behind. A delta against last week is shown only when
 * *both* weeks hold real data for that metric — last week's silence is her not using the app yet,
 * not a week of zero water or no sleep, and reporting "+400ml vs last week" against nothing would be
 * a lie dressed as encouragement.
 */
object WeeklySummaryLogic {

    fun compute(
        logsByDate: Map<LocalDate, DailyLog>,
        today: LocalDate = LocalDate.now(),
        plan: List<Supplement> = Supplement.defaultPlan,
    ): WeeklySummaryInsights {
        val thisWeek = WeekBuckets.weekDays(today)
        val lastWeek = WeekBuckets.weekDays(today.minusWeeks(1))

        val thisLogs = thisWeek.mapNotNull { logsByDate[it]?.takeIf(DailyLog::isMeaningful) }
        if (thisLogs.isEmpty()) return WeeklySummaryInsights()

        val prevLogs = lastWeek.mapNotNull { logsByDate[it]?.takeIf(DailyLog::isMeaningful) }
        val daysLogged = thisLogs.size
        val prevDaysLogged = prevLogs.size

        val topMood = thisLogs.mapNotNull { it.mood }
            .groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
        val topEnergy = thisLogs.mapNotNull { it.energy }
            .groupingBy { it }.eachCount().maxByOrNull { it.value }?.key

        return WeeklySummaryInsights(
            hasData = true,
            daysLogged = daysLogged,
            prevDaysLogged = prevDaysLogged,
            hydrationDeltaMlPerDay = hydrationDelta(logsByDate, thisWeek, lastWeek),
            sleepDeltaMinutes = sleepDelta(logsByDate, thisWeek, lastWeek),
            supplementDaysDelta = supplementDaysDelta(logsByDate, thisWeek, lastWeek, plan),
            moodEnergyLine = moodEnergyLine(topMood, topEnergy),
            insight = narrativeFor(daysLogged, prevDaysLogged),
        )
    }

    /** Average ml/day, where an unlogged day is genuinely zero water drunk (as Hydration counts it). */
    private fun hydrationDelta(
        logs: Map<LocalDate, DailyLog>,
        thisWeek: List<LocalDate>,
        lastWeek: List<LocalDate>,
    ): Int? {
        fun hasWater(week: List<LocalDate>) = week.any { (logs[it]?.waterMl ?: 0) > 0 }
        if (!hasWater(thisWeek) || !hasWater(lastWeek)) return null
        fun avg(week: List<LocalDate>) = week.sumOf { logs[it]?.waterMl ?: 0 } / 7
        return avg(thisWeek) - avg(lastWeek)
    }

    /** Average over nights *logged*, never over seven — an unlogged night is not a night of no sleep. */
    private fun sleepDelta(
        logs: Map<LocalDate, DailyLog>,
        thisWeek: List<LocalDate>,
        lastWeek: List<LocalDate>,
    ): Int? {
        fun nights(week: List<LocalDate>) =
            week.mapNotNull { logs[it]?.sleepMinutes?.takeIf { m -> m > 0 } }
        val thisN = nights(thisWeek)
        val lastN = nights(lastWeek)
        if (thisN.isEmpty() || lastN.isEmpty()) return null
        return thisN.sum() / thisN.size - lastN.sum() / lastN.size
    }

    /** Days on which she took at least one supplement from her plan. */
    private fun supplementDaysDelta(
        logs: Map<LocalDate, DailyLog>,
        thisWeek: List<LocalDate>,
        lastWeek: List<LocalDate>,
        plan: List<Supplement>,
    ): Int? {
        if (plan.isEmpty()) return null
        fun days(week: List<LocalDate>) = week.count { date ->
            val logged = logs[date]?.supplements.orEmpty().mapNotNull(Supplement::fromWire)
            plan.any { it in logged }
        }
        val thisD = days(thisWeek)
        val lastD = days(lastWeek)
        if (thisD == 0 && lastD == 0) return null
        return thisD - lastD
    }

    /** "Mostly good, energy often high" — a tally, never a verdict. Empty when neither was logged. */
    private fun moodEnergyLine(topMood: Mood?, topEnergy: EnergyLevel?): String {
        val mood = topMood?.let { "mostly ${it.label.lowercase()}" }
        val energy = topEnergy?.let { "energy often ${energyWord(it)}" }
        return listOfNotNull(mood, energy).joinToString(", ")
            .replaceFirstChar { it.uppercase() }
    }

    private fun energyWord(level: EnergyLevel): String = when (level) {
        EnergyLevel.LOW -> "low"
        EnergyLevel.NORMAL -> "steady"
        EnergyLevel.HIGH -> "high"
    }

    /** Counts what she did against last week, and — when she did less — never frames it as a failure. */
    private fun narrativeFor(daysLogged: Int, prevDaysLogged: Int): String {
        val base = "You logged $daysLogged of 7 days this week"
        return when {
            prevDaysLogged == 0 -> "$base. A steady first week to build on."
            daysLogged > prevDaysLogged ->
                "$base — ${daysLogged - prevDaysLogged} more than last week. Lovely momentum."
            daysLogged == prevDaysLogged -> "$base, right in step with last week."
            else -> "$base. Every day you note is one more you can look back on."
        }
    }
}
