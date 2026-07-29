package com.genesyx.app.ui.track

import com.genesyx.app.domain.model.LogDay

/**
 * The categories a calendar day can advertise. The cell is ~40dp, so this is deliberately four
 * coarse groups rather than one marker per field: enough to answer "did I log anything, and roughly
 * what?" at a glance, with the day-detail dialog carrying the actual values.
 *
 * Order is the render order and is fixed, so a day's dots don't rearrange between recompositions.
 */
enum class DayMarker { LOG, WATER, SUPPLEMENTS, PH }

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

        // LOG covers the "how you felt" fields. Water and supplements are pulled out separately
        // because they are the two the user acts on daily and most wants to see at a glance.
        if (log != null && with(log) {
                mood != null || energy != null || symptoms.isNotEmpty() ||
                    sleepMinutes != null || !notes.isNullOrBlank()
            }
        ) {
            markers += DayMarker.LOG
        }
        if (log != null && log.waterMl > 0) markers += DayMarker.WATER
        if (log != null && log.supplements.isNotEmpty()) markers += DayMarker.SUPPLEMENTS
        if (logDay.phReadings.isNotEmpty()) markers += DayMarker.PH

        return markers
    }
}
