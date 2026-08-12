package com.genesyx.app.ui.track

import com.genesyx.app.domain.model.LogDay

/**
 * The categories a calendar day can advertise. The cell is ~40dp, so these are deliberately coarse
 * groups rather than one marker per field: enough to answer "did I log anything, and roughly
 * what?" at a glance, with the day-detail dialog carrying the actual values.
 *
 * Order is the render order and is fixed, so a day's dots don't rearrange between recompositions.
 */
enum class DayMarker { LOG, SYMPTOMS, WATER, SUPPLEMENTS, PH, ACTIVITY }

/**
 * Which markers a day earns. Split out from the composable so the rules are unit-testable — the
 * failure that matters is a day showing dots it didn't earn (or none it did), and that is much
 * easier to pin here than through a Compose test.
 */
object DayMarkers {

    fun forDay(logDay: LogDay?): List<DayMarker> {
        if (logDay == null) return emptyList()
        val markers = mutableListOf<DayMarker>()
        val log = logDay.dailyLog

        // LOG covers how she felt (mood/energy/sleep). Symptoms & notes used to fold into it, but
        // "which days had symptoms" is exactly what a calendar scan is for, so they earn their own
        // dot. Water and supplements stay separate — the two she acts on daily.
        if (log != null && with(log) { mood != null || energy != null || sleepMinutes != null }) {
            markers += DayMarker.LOG
        }
        if (log != null && (log.symptoms.isNotEmpty() || !log.notes.isNullOrBlank())) {
            markers += DayMarker.SYMPTOMS
        }
        if (log != null && log.waterMl > 0) markers += DayMarker.WATER
        if (log != null && log.supplements.isNotEmpty()) markers += DayMarker.SUPPLEMENTS
        if (logDay.phReadings.isNotEmpty()) markers += DayMarker.PH
        if (log?.sexualActivity == true) markers += DayMarker.ACTIVITY

        return markers
    }
}
