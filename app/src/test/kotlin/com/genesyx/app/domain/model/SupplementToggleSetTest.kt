package com.genesyx.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The toggle set the Nutrition card, the Track summary and the Insights card all score against. */
class SupplementToggleSetTest {

    private val plan = SupplementToggleSet.build(emptyList())

    @Test
    fun `the plan is the four bundled essentials, storing their wire names`() {
        assertEquals(listOf("Folate", "Omega-3", "Vitamin D", "Zinc"), plan.map { it.display })
        assertEquals(listOf("Folic acid", "Omega-3", "Vitamin D", "Zinc"), plan.map { it.stored })
        assertEquals(listOf("F", "O", "D", "Z"), plan.map { it.initial })
        assertEquals("400–800 mcg", plan.first().dose)
        assertTrue(plan.all { !it.isCustom })
    }

    @Test
    fun `nothing logged reads None logged yet today`() {
        val taken = SupplementToggleSet.takenCount(plan, emptySet())
        assertEquals(0, taken)
        assertEquals("None logged yet today", SupplementToggleSet.statusLine(taken, plan.size))
    }

    @Test
    fun `one plan supplement logged reads one of the plan size`() {
        val taken = SupplementToggleSet.takenCount(plan, setOf("Folic acid"))
        assertEquals(1, taken)
        assertEquals("1 of 4 logged today", SupplementToggleSet.statusLine(taken, plan.size))
    }

    @Test
    fun `matching ignores case and surrounding whitespace`() {
        // An older build or the other platform may store the wire names less tidily.
        assertEquals(2, SupplementToggleSet.takenCount(plan, setOf("  folic ACID ", "vitamin d")))
    }

    @Test
    fun `supplements outside the set are recorded, not scored`() {
        // Iron is loggable but sits outside the suggested plan; unknown strings don't score.
        assertEquals(1, SupplementToggleSet.takenCount(plan, setOf("Iron", "Magnesium", "Zinc")))
    }

    @Test
    fun `blank entries never match`() {
        assertEquals(0, SupplementToggleSet.takenCount(plan, setOf("", "   ")))
    }

    @Test
    fun `her own supplement joins the set and the denominator`() {
        val set = SupplementToggleSet.build(listOf(UserSupplement(name = "Magnesium", dose = "300 mg")))
        assertEquals(5, set.size)
        val custom = set.last()
        assertTrue(custom.isCustom)
        assertEquals("Magnesium", custom.display)
        assertEquals("Magnesium", custom.stored)
        assertEquals("M", custom.initial)
        assertEquals("300 mg", custom.dose)
        assertNull(custom.builtIn)

        val taken = SupplementToggleSet.takenCount(set, setOf("Folic acid", "magnesium"))
        assertEquals(2, taken)
        assertEquals("2 of 5 logged today", SupplementToggleSet.statusLine(taken, set.size))
    }

    @Test
    fun `a custom entry that names a plan item is one chip, not two`() {
        // By wire name, by display name, in any case — the plan chip already covers it.
        val set = SupplementToggleSet.build(
            listOf(
                UserSupplement(name = "Folic acid"),
                UserSupplement(name = "folate"),
                UserSupplement(name = "VITAMIN D"),
                UserSupplement(name = "Magnesium"),
                UserSupplement(name = "magnesium "),
                UserSupplement(name = "   "),
            ),
        )
        assertEquals(listOf("Folate", "Omega-3", "Vitamin D", "Zinc", "Magnesium"), set.map { it.display })
    }

    @Test
    fun `a custom iron entry is allowed — iron is not in the plan`() {
        val set = SupplementToggleSet.build(listOf(UserSupplement(name = "Iron")))
        assertEquals(5, set.size)
        assertFalse(set.last().display == "Zinc")
        assertEquals("Iron", set.last().stored)
    }
}
