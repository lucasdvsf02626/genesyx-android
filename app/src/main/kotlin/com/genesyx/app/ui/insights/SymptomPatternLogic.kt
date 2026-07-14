package com.genesyx.app.ui.insights

import com.genesyx.app.domain.model.DailyLog
import java.time.LocalDate

/**
 * Pure symptom-pattern computation: a 28-day heatmap, plus the one symptom she logs most.
 *
 * The grid covers the last 28 days ending today, oldest first, so it fills four rows of seven. The
 * *summary*, by contrast, counts her whole history — a symptom she logged heavily two months ago is
 * still her most-logged symptom, and the copy says "in all" so the number is never mistaken for the
 * window on screen.
 *
 * The thin-data guard is the point of this file. Four scattered symptom days are noise, and a card
 * that announced a "pattern" from them would be inventing one. Below [MIN_DAYS_FOR_PATTERN] days
 * the card shows the grid and explicitly declines to read anything into it.
 */
object SymptomPatternLogic {

    const val WINDOW_DAYS = 28

    /** Under this many symptom days, the card names no pattern. Four weeks of grid, one week of data. */
    const val MIN_DAYS_FOR_PATTERN = 7

    fun compute(
        logsByDate: Map<LocalDate, DailyLog>,
        today: LocalDate = LocalDate.now(),
    ): SymptomPatternInsights {
        // Oldest → newest, so the grid reads like a calendar and today lands in the bottom-right.
        val window = (WINDOW_DAYS - 1 downTo 0).map { today.minusDays(it.toLong()) }
        val heatmapValues = window.map { logsByDate[it]?.symptoms?.count(::isReal) ?: 0 }

        // Frequency and the day count come from her whole history, not just the window.
        val symptomDays = logsByDate.values.filter { log -> log.symptoms.any(::isReal) }
        val daysWithSymptoms = symptomDays.size

        if (daysWithSymptoms == 0) return SymptomPatternInsights(heatmapValues = heatmapValues)

        val top = topSymptom(symptomDays)
        val hasEnoughData = daysWithSymptoms >= MIN_DAYS_FOR_PATTERN

        return SymptomPatternInsights(
            heatmapValues = heatmapValues,
            daysWithSymptoms = daysWithSymptoms,
            topSymptom = top?.first,
            topCount = top?.second ?: 0,
            hasEnoughData = hasEnoughData,
            insight = when {
                !hasEnoughData ->
                    "Early days — too soon to read patterns. Keep logging and this will fill out."
                top == null -> SymptomPatternInsights().insight
                else ->
                    "${top.first} is the symptom you log most — ${top.second} " +
                        "${if (top.second == 1) "day" else "days"} in all, of the $daysWithSymptoms " +
                        "days you've logged a symptom."
            },
        )
    }

    /** Blank strings can reach the set through the Log's free-text field; they are not symptoms. */
    private fun isReal(symptom: String) = symptom.isNotBlank()

    /**
     * The most-logged symptom, ties broken alphabetically. The count is *days*, matching the copy.
     *
     * Names are grouped case-insensitively: the Log offers capitalised chips but also a free-text
     * field, so "Cramps" and "cramps" can both be stored — counting them apart would split her real
     * total in half and could hand the top spot to the wrong symptom. Each day is then counted once
     * per name (a day holding both spellings is still one day of cramps, not two). The group is
     * displayed under its alphabetically-first spelling, which keeps the answer deterministic.
     */
    private fun topSymptom(symptomDays: List<DailyLog>): Pair<String, Int>? =
        symptomDays
            .flatMap { log -> log.symptoms.filter(::isReal).map { it.trim() }.distinctBy(String::lowercase) }
            .groupBy { it.lowercase() }
            .map { (key, spellings) -> Triple(spellings.min(), spellings.size, key) }
            .sortedWith(compareByDescending<Triple<String, Int, String>> { it.second }.thenBy { it.third })
            .firstOrNull()
            ?.let { it.first to it.second }
}
