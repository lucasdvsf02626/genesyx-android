package com.genesyx.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The vocabulary, and the boundary it defends.
 *
 * `daily_logs.supplements` is a shared `text[]` column: iOS writes and reads the same strings. The
 * wire-name test below is the guard on that contract — if someone "tidies" a wireName, this fails
 * loudly rather than silently scoring every Android-logged day as zero on an iPhone.
 */
class SupplementTest {

    @Test
    fun `wire names are the strings already in the database and must not drift`() {
        // These four are what v1.0/v1.1 wrote from the Log screen. Changing any of them orphans
        // every row already stored on the server and breaks iOS, which reads this column too.
        assertEquals("Folic acid", Supplement.FOLATE.wireName)
        assertEquals("Vitamin D", Supplement.VITAMIN_D.wireName)
        assertEquals("Omega-3", Supplement.OMEGA_3.wireName)
        assertEquals("Iron", Supplement.IRON.wireName)
    }

    @Test
    fun `every legacy string maps back to the vocabulary`() {
        val legacy = listOf("Folic acid", "Vitamin D", "Omega-3", "Iron")
        assertEquals(
            listOf(Supplement.FOLATE, Supplement.VITAMIN_D, Supplement.OMEGA_3, Supplement.IRON),
            legacy.map { Supplement.fromWire(it) },
        )
    }

    @Test
    fun `an unknown string is not guessed at`() {
        // An older build, or another client. It must not be coerced into a neighbour.
        assertNull(Supplement.fromWire("Folate"))
        assertNull(Supplement.fromWire("folate"))
        assertNull(Supplement.fromWire(""))
    }

    @Test
    fun `dosage is held apart from the name`() {
        // The bug this type exists to kill: "Folate (400–800 mcg)" was never going to match
        // "Folic acid", because the dosage was inside the string being compared.
        assertEquals("Folate", Supplement.FOLATE.displayName)
        assertEquals("400–800 mcg", Supplement.FOLATE.dosageNote)
        assertFalse(Supplement.FOLATE.displayName.contains("mcg"))
        assertNull("iron carries no dosage", Supplement.IRON.dosageNote)
    }

    @Test
    fun `iron is loggable but sits outside the default plan`() {
        assertTrue(Supplement.IRON in Supplement.loggable)
        assertFalse("taking iron must not score against a plan it is not in", Supplement.IRON in Supplement.defaultPlan)
        assertEquals(
            listOf(Supplement.FOLATE, Supplement.OMEGA_3, Supplement.VITAMIN_D, Supplement.ZINC),
            Supplement.defaultPlan,
        )
        assertEquals(5, Supplement.loggable.size)
    }

    @Test
    fun `ids are stable and unique`() {
        val ids = Supplement.entries.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        assertEquals(listOf("folate", "omega3", "vitamin_d", "zinc", "iron"), ids)
    }
}
