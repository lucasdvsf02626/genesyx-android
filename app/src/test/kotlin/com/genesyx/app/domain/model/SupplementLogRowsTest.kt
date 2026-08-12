package com.genesyx.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Guards the rule that a supplement string once logged is never left invisible in the dialog. */
class SupplementLogRowsTest {

    private val builtInWire = Supplement.loggable.map { it.wireName }.toSet()

    @Test
    fun `built-ins always lead, toggling their wire names`() {
        val rows = SupplementLogRows.forDay(selected = emptySet(), custom = emptyList())
        assertEquals(Supplement.loggable.size, rows.size)
        assertEquals(Supplement.loggable.map { it.wireName }, rows.map { it.stored })
        assertEquals(Supplement.loggable.map { it.displayName }, rows.map { it.display })
    }

    @Test
    fun `current custom entries follow the built-ins`() {
        val rows = SupplementLogRows.forDay(selected = emptySet(), custom = listOf("Magnesium"))
        val custom = rows.last()
        assertEquals("Magnesium", custom.display)
        assertEquals("Magnesium", custom.stored)
        assertEquals(Supplement.loggable.size + 1, rows.size)
    }

    @Test
    fun `a renamed custom supplement's old log stays visible as an orphan row`() {
        // She logged "Magnesium", then renamed the entry to "Magnesium Glycinate". The old string
        // is still stored on that day and must remain visible and untoggleable-away.
        val rows = SupplementLogRows.forDay(
            selected = setOf("Magnesium"),
            custom = listOf("Magnesium Glycinate"),
        )
        val stored = rows.map { it.stored }
        assertTrue("current entry shown", "Magnesium Glycinate" in stored)
        assertTrue("orphaned log preserved", "Magnesium" in stored)
    }

    @Test
    fun `a stored built-in wire name never becomes a duplicate orphan row`() {
        val folateWire = Supplement.FOLATE.wireName // "Folic acid" — the wire string, not the id
        val rows = SupplementLogRows.forDay(selected = setOf(folateWire), custom = emptyList())
        assertEquals(
            "the built-in appears exactly once (its built-in row)",
            1,
            rows.count { it.stored == folateWire },
        )
        assertEquals(Supplement.loggable.size, rows.size)
    }

    @Test
    fun `orphans are deterministic and only ever extra strings`() {
        val folateWire = Supplement.FOLATE.wireName
        val rows = SupplementLogRows.forDay(
            selected = setOf("Zebra", "Apple", folateWire),
            custom = listOf("Apple"),
        )
        val orphanRows = rows.drop(Supplement.loggable.size + 1) // built-ins + one custom
        assertEquals(listOf("Zebra"), orphanRows.map { it.stored }) // Apple is custom, folate built-in
        // Every row's stored string is real: a built-in, a custom, or a selected orphan.
        rows.forEach {
            assertTrue(it.stored in builtInWire || it.stored == "Apple" || it.stored == "Zebra")
        }
    }
}
