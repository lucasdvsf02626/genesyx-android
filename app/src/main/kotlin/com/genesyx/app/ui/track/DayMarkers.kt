package com.genesyx.app.ui.track

import com.genesyx.app.domain.model.LogDay

/**
 * The categories a calendar day can advertise. Deliberately the three highest-signal ones —
 * pH, symptoms/notes, intimacy — matching the iOS Track calendar (cross-platform design parity,
 * 12 Aug 2026). Everyday logging (mood/energy/sleep, water, supplements) is still recorded and shown
 * in the day-detail dialog; it just doesn't crowd the ~40dp cell with a dot each.
 *
 * Order is the render order and is fixed (matches iOS `DayMarker.allCases`), so a day's dots don't
 * rearrange between recompositions.
 */
enum class DayMarker { PH, SYMPTOMS, ACTIVITY }

/**
 * Which markers a day earns. Split out from the composable so the rules are unit-testable — the
 * failure that matters is a day showing dots it didn't earn (or none it did), and that is much
 * easier to pin here than through a Compose test. Rules mirror iOS `DayMarkers.markers`.
 */
object DayMarkers {

    fun forDay(logDay: LogDay?): List<DayMarker> {
        if (logDay == null) return emptyList()
        val markers = mutableListOf<DayMarker>()
        val log = logDay.dailyLog

        if (logDay.phReadings.isNotEmpty()) markers += DayMarker.PH
        if (log != null && (log.symptoms.isNotEmpty() || !log.notes.isNullOrBlank())) {
            markers += DayMarker.SYMPTOMS
        }
        if (log?.sexualActivity == true) markers += DayMarker.ACTIVITY

        return markers
    }
}
