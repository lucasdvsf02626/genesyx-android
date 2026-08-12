package com.genesyx.app.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/**
 * Date resolution for the log editor. The bug this guards against is an editor that writes to the
 * wrong row — a calendar tap on last Tuesday must edit last Tuesday, and a malformed or future
 * date must fall back to today rather than open an editor over a prediction.
 */
class LogViewModelTest {

    private val today = LocalDate.of(2026, 8, 10)

    @Test
    fun `no date argument means today`() {
        assertEquals(today, LogViewModel.resolveDate(null, today))
    }

    @Test
    fun `a past date is edited as itself`() {
        assertEquals(LocalDate.of(2026, 8, 4), LogViewModel.resolveDate("2026-08-04", today))
    }

    @Test
    fun `today passed explicitly is today`() {
        assertEquals(today, LogViewModel.resolveDate("2026-08-10", today))
    }

    @Test
    fun `a future date falls back to today`() {
        // Predictions aren't loggable; a crafted deep link must not write into the future.
        assertEquals(today, LogViewModel.resolveDate("2026-08-11", today))
    }

    @Test
    fun `garbage falls back to today`() {
        assertEquals(today, LogViewModel.resolveDate("not-a-date", today))
        assertEquals(today, LogViewModel.resolveDate("", today))
    }
}
