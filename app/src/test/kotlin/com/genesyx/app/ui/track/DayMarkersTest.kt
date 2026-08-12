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
 * Calendar day markers. The bug these guard against is a cell that lies — dots for a day with
 * nothing logged, or a bare cell on a day the user did log — which is exactly the "I can't see what
 * I logged" complaint the markers exist to fix.
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
        // A row can exist with every field at its default — that is not "logged".
        assertEquals(emptyList<DayMarker>(), DayMarkers.forDay(day(log = DailyLog())))
    }

    @Test
    fun `water alone earns only the water marker`() {
        assertEquals(listOf(DayMarker.WATER), DayMarkers.forDay(day(log = DailyLog(waterMl = 250))))
    }

    @Test
    fun `zero water is not a water marker`() {
        assertEquals(emptyList<DayMarker>(), DayMarkers.forDay(day(log = DailyLog(waterMl = 0))))
    }

    @Test
    fun `mood and energy collapse into the single log marker`() {
        val log = DailyLog(mood = Mood.GOOD, energy = EnergyLevel.HIGH)
        assertEquals(listOf(DayMarker.LOG), DayMarkers.forDay(day(log = log)))
    }

    @Test
    fun `symptoms earn their own marker, not the log one`() {
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
    fun `a logged zero-minute night still counts as logged`() {
        // Mirrors DailyLog.isMeaningful: null means "not entered", 0 means "I was up all night".
        assertEquals(listOf(DayMarker.LOG), DayMarkers.forDay(day(log = DailyLog(sleepMinutes = 0))))
    }

    @Test
    fun `a blank note is not content`() {
        assertEquals(emptyList<DayMarker>(), DayMarkers.forDay(day(log = DailyLog(notes = "   "))))
    }

    @Test
    fun `a ph reading alone earns a marker even with no daily log`() {
        assertEquals(listOf(DayMarker.PH), DayMarkers.forDay(day(ph = listOf(reading()))))
    }

    @Test
    fun `a legacy urine reading still marks the day`() {
        // The day-detail dialog shows it as "urine (legacy)". The cell's job is only to say
        // "something is here" — hiding it would strand the reading with no way to reach it.
        val legacy = reading(6.5).copy(measurementType = "urine")
        assertEquals(listOf(DayMarker.PH), DayMarkers.forDay(day(ph = listOf(legacy))))
    }

    @Test
    fun `a full day renders every marker in a fixed order`() {
        val log = DailyLog(
            mood = Mood.GREAT,
            supplements = setOf("Folic acid"),
            waterMl = 2000,
        )
        assertEquals(
            listOf(DayMarker.LOG, DayMarker.WATER, DayMarker.SUPPLEMENTS, DayMarker.PH),
            DayMarkers.forDay(day(log = log, ph = listOf(reading()))),
        )
    }

    @Test
    fun `markers never exceed what a cell can show`() {
        val log = DailyLog(
            mood = Mood.GREAT, energy = EnergyLevel.HIGH, symptoms = setOf("a", "b"),
            sleepMinutes = 400, supplements = setOf("Zinc"), notes = "note", waterMl = 2000,
            sexualActivity = true,
        )
        val markers = DayMarkers.forDay(day(log = log, ph = listOf(reading(), reading())))
        // The cell renders dots in rows of three, so six is the ceiling the grid was sized for.
        assertEquals("A cell can render at most six dots (two rows of three)", 6, markers.size)
    }
}
