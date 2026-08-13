package com.genesyx.app.ui.insights

import com.genesyx.app.domain.model.DailyLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class UserSupplementInsightLogicTest {

    // A Wednesday, so the Mon–Sun week is 2026-08-10 .. 2026-08-16.
    private val today = LocalDate.of(2026, 8, 12)
    private val monday = LocalDate.of(2026, 8, 10)

    private fun logWith(date: LocalDate, supplements: Set<String>) =
        date to DailyLog(supplements = supplements)

    @Test
    fun `no supplements means the section is hidden`() {
        val result = UserSupplementInsightLogic.compute(emptyList(), emptyMap(), today)
        assertFalse(result.hasSupplements)
        assertTrue(result.rows.isEmpty())
    }

    @Test
    fun `a supplement never logged still lists at zero of seven`() {
        val result = UserSupplementInsightLogic.compute(listOf("Magnesium"), emptyMap(), today)
        assertTrue(result.hasSupplements)
        assertEquals(1, result.rows.size)
        assertEquals("Magnesium", result.rows[0].name)
        assertEquals(0, result.rows[0].daysLogged)
        assertEquals(List(7) { false }, result.rows[0].perDay)
    }

    @Test
    fun `counts the days a supplement appears in the log, case-insensitively`() {
        val logs = mapOf(
            logWith(monday, setOf("Folate test")),                          // Mon: folate
            logWith(monday.plusDays(2), setOf("folate TEST", "Magnesium")), // Wed: both (folate diff case)
        )

        val result = UserSupplementInsightLogic.compute(listOf("Folate test", "Magnesium"), logs, today)

        val folate = result.rows.first { it.name == "Folate test" }
        assertEquals(2, folate.daysLogged)                       // Mon + Wed
        assertEquals(listOf(true, false, true, false, false, false, false), folate.perDay)

        val magnesium = result.rows.first { it.name == "Magnesium" }
        assertEquals(1, magnesium.daysLogged)                    // Wed only
    }

    @Test
    fun `duplicate names are collapsed case-insensitively`() {
        val result = UserSupplementInsightLogic.compute(listOf("Zinc", "zinc", " Zinc "), emptyMap(), today)
        assertEquals(1, result.rows.size)
    }
}
