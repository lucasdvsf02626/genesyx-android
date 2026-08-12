package com.genesyx.app.ui.track

import com.genesyx.app.domain.model.DailyLog
import com.genesyx.app.domain.model.EnergyLevel
import com.genesyx.app.domain.model.LogDay
import com.genesyx.app.domain.model.Mood
import com.genesyx.app.domain.model.PhReading
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Calendar day markers — the three highest-signal ones (pH, symptoms/notes, intimacy), matching the
 * iOS Track calendar. The bug these guard against is a cell that lies: a dot for a signal that
 * isn't there, or a missing dot for one that is. Everyday logging (mood/energy/sleep, water,
 * supplements) deliberately earns no dot — it's shown in the day-detail dialog, not on the cell.
 */
class DayMarkersTest {

    private val date = LocalDate.of(2026, 7, 29)

    private fun day(log: DailyLog? = null, ph: List<PhReading> = emptyList()) =
        LogDay(date = date, dailyLog = log, phReadings = ph)

    private fun reading(value: Double = 4.2) =
        PhReading(phValue = value, recordedAt = LocalDateTime.of(2026, 7, 29, 9, 0))

    @Test
    fun `a day with no data earns no markers`() {
        assertEquals(emptyList<DayMarker>(), DayMarkers.forDay(null))
        assertEquals(emptyList<DayMarker>(), DayMarkers.forDay(day()))
    }

    @Test
    fun `an empty shell of a log earns no markers`() {
        assertEquals(emptyList<DayMarker>(), DayMarkers.forDay(day(log = DailyLog())))
    }

    @Test
    fun `everyday logging without a marker signal earns no dot`() {
        // Mood, energy, sleep, water and supplements are recorded but not marked on the cell —
        // matching iOS. They surface in the day-detail dialog.
        val log = DailyLog(
            mood = Mood.GOOD,
            energy = EnergyLevel.HIGH,
            sleepMinutes = 400,
            waterMl = 2000,
            supplements = setOf("Folic acid"),
        )
        assertEquals(emptyList<DayMarker>(), DayMarkers.forDay(day(log = log)))
    }

    @Test
    fun `symptoms earn the symptoms marker`() {
        assertEquals(
            listOf(DayMarker.SYMPTOMS),
            DayMarkers.forDay(day(log = DailyLog(symptoms = setOf("cramps")))),
        )
    }

    @Test
    fun `a note alone earns the symptoms marker`() {
        assertEquals(
            listOf(DayMarker.SYMPTOMS),
            DayMarkers.forDay(day(log = DailyLog(notes = "remember this"))),
        )
    }

    @Test
    fun `a blank note is not content`() {
        assertEquals(emptyList<DayMarker>(), DayMarkers.forDay(day(log = DailyLog(notes = "   "))))
    }

    @Test
    fun `intimacy earns its own marker`() {
        assertEquals(
            listOf(DayMarker.ACTIVITY),
            DayMarkers.forDay(day(log = DailyLog(sexualActivity = true))),
        )
    }

    @Test
    fun `intimacy recorded as false or not recorded earns no marker`() {
        assertEquals(emptyList<DayMarker>(), DayMarkers.forDay(day(log = DailyLog(sexualActivity = false))))
        assertEquals(emptyList<DayMarker>(), DayMarkers.forDay(day(log = DailyLog(sexualActivity = null))))
    }

    @Test
    fun `a ph reading alone earns a marker even with no daily log`() {
        assertEquals(listOf(DayMarker.PH), DayMarkers.forDay(day(ph = listOf(reading()))))
    }

    @Test
    fun `a legacy urine reading still marks the day`() {
        val legacy = reading(6.5).copy(measurementType = "urine")
        assertEquals(listOf(DayMarker.PH), DayMarkers.forDay(day(ph = listOf(legacy))))
    }

    @Test
    fun `a full day renders the three markers in a fixed order`() {
        val log = DailyLog(
            symptoms = setOf("cramps"),
            notes = "note",
            sexualActivity = true,
            // These are recorded but do not earn dots.
            mood = Mood.GREAT,
            waterMl = 2000,
            supplements = setOf("Zinc"),
        )
        assertEquals(
            listOf(DayMarker.PH, DayMarker.SYMPTOMS, DayMarker.ACTIVITY),
            DayMarkers.forDay(day(log = log, ph = listOf(reading()))),
        )
    }

    @Test
    fun `markers never exceed what a cell can show`() {
        val log = DailyLog(symptoms = setOf("a"), sexualActivity = true)
        val markers = DayMarkers.forDay(day(log = log, ph = listOf(reading())))
        // Three signals — a single row of three dots in a ~40dp cell.
        assertEquals(3, markers.size)
    }
}
