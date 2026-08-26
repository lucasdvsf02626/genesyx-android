package com.genesyx.app.ui.track.detail

import com.genesyx.app.domain.content.FoodGroup
import com.genesyx.app.domain.model.DailyLog
import com.genesyx.app.domain.model.Supplement
import com.genesyx.app.domain.time.WeekBuckets
import java.time.LocalDate

/** What the Nutrition tracker (Track → Nutrition) shows: today's names and this week's dots. */
data class NutritionTrackerSummary(
    /** Supplement names logged today, display names where the string is a known wire name. */
    val todaySupplements: List<String> = emptyList(),
    /** Food groups logged today, as labels. */
    val todayFoodGroups: List<String> = emptyList(),
    /** Mon..Sun of the current week: how many supplements were logged each day. */
    val weekSupplementCounts: List<Int> = List(7) { 0 },
    /** Mon..Sun of the current week: how many food groups were logged each day. */
    val weekFoodGroupCounts: List<Int> = List(7) { 0 },
) {
    /** True when no supplement was logged on any day this week — the only time the footer shows. */
    val supplementWeekEmpty: Boolean get() = weekSupplementCounts.all { it == 0 }
}

/**
 * A read-only summary of `daily_logs` for the tracker sheet. Logging happens on the Nutrition tab;
 * this only reads the rows it wrote. Everything she stored is shown — a supplement logged under a
 * name this build does not recognise is listed by its stored string, never dropped.
 */
object NutritionTrackerLogic {

    fun compute(
        logsByDate: Map<LocalDate, DailyLog>,
        today: LocalDate = LocalDate.now(),
    ): NutritionTrackerSummary {
        val week = WeekBuckets.weekDays(today)
        val todayLog = logsByDate[today]
        return NutritionTrackerSummary(
            todaySupplements = supplementNames(todayLog),
            todayFoodGroups = foodGroupLabels(todayLog),
            weekSupplementCounts = week.map { supplementNames(logsByDate[it]).size },
            weekFoodGroupCounts = week.map { foodGroupLabels(logsByDate[it]).size },
        )
    }

    private fun supplementNames(log: DailyLog?): List<String> =
        log?.supplements.orEmpty()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { Supplement.fromWire(it)?.displayName ?: it }
            .distinct()

    private fun foodGroupLabels(log: DailyLog?): List<String> =
        log?.foodGroups.orEmpty()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { FoodGroup.fromRaw(it)?.label ?: it }
            .distinct()
}
