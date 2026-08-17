package com.genesyx.app.domain.cycle

import com.genesyx.app.domain.model.Phase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PhaseChangeTest {

    private val today = LocalDate.of(2026, 8, 17)

    @Test
    fun `first setup is not a phase-entry moment`() {
        val d = PhaseChange.evaluate(Phase.FOLLICULAR, storedPhase = null, storedEpochDay = null, today = today)
        assertFalse(d.justChanged)
        assertEquals(Phase.FOLLICULAR.name, d.persistPhase)
        assertEquals(today.toEpochDay(), d.persistEpochDay)
    }

    @Test
    fun `a new phase is a phase-entry moment and is persisted`() {
        val d = PhaseChange.evaluate(
            Phase.OVULATORY,
            storedPhase = Phase.FOLLICULAR.name,
            storedEpochDay = today.minusDays(5).toEpochDay(),
            today = today,
        )
        assertTrue(d.justChanged)
        assertEquals(Phase.OVULATORY.name, d.persistPhase)
    }

    @Test
    fun `the first day of a phase keeps the entry card without rewriting`() {
        val d = PhaseChange.evaluate(
            Phase.OVULATORY,
            storedPhase = Phase.OVULATORY.name,
            storedEpochDay = today.toEpochDay(),
            today = today,
        )
        assertTrue(d.justChanged)
        assertNull(d.persistPhase)
    }

    @Test
    fun `a later day in the same phase is not an entry moment`() {
        val d = PhaseChange.evaluate(
            Phase.OVULATORY,
            storedPhase = Phase.OVULATORY.name,
            storedEpochDay = today.minusDays(1).toEpochDay(),
            today = today,
        )
        assertFalse(d.justChanged)
    }
}
