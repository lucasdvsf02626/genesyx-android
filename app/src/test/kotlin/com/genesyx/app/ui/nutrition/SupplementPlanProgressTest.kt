package com.genesyx.app.ui.nutrition

import com.genesyx.app.domain.content.supplementPlan
import org.junit.Assert.assertEquals
import org.junit.Test

class SupplementPlanProgressTest {

    @Test
    fun `nothing logged reads None logged yet today`() {
        val taken = SupplementPlanProgress.takenToday(emptySet())
        assertEquals(0, taken)
        assertEquals("None logged yet today", SupplementPlanProgress.statusLine(taken))
    }

    @Test
    fun `one plan supplement logged reads one of the plan size`() {
        val taken = SupplementPlanProgress.takenToday(setOf("Folic acid"))
        assertEquals(1, taken)
        assertEquals("1 of ${supplementPlan.size} taken today", SupplementPlanProgress.statusLine(taken))
    }

    @Test
    fun `the whole plan logged counts every item`() {
        val taken = SupplementPlanProgress.takenToday(setOf("Folic acid", "Omega-3", "Vitamin D", "Zinc"))
        assertEquals(supplementPlan.size, taken)
    }

    @Test
    fun `matching ignores case and surrounding whitespace`() {
        // An older build or the other platform may store the wire names less tidily.
        val taken = SupplementPlanProgress.takenToday(setOf("  folic ACID ", "vitamin d"))
        assertEquals(2, taken)
    }

    @Test
    fun `supplements outside the plan are recorded, not scored`() {
        // Iron is loggable but sits outside the suggested plan; her own entries don't score either.
        val taken = SupplementPlanProgress.takenToday(setOf("Iron", "Magnesium", "Zinc"))
        assertEquals(1, taken)
    }

    @Test
    fun `blank entries never match`() {
        assertEquals(0, SupplementPlanProgress.takenToday(setOf("", "   ")))
    }
}
