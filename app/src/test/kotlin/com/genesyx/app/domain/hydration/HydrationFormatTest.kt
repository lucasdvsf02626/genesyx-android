package com.genesyx.app.domain.hydration

import org.junit.Assert.assertEquals
import org.junit.Test

/** One rule everywhere: sub-litre in ml, litre-and-up in litres to one decimal. */
class HydrationFormatTest {

    @Test
    fun `below a litre reads in millilitres`() {
        assertEquals("0ml", HydrationFormat.format(0))
        assertEquals("600ml", HydrationFormat.format(600))
        assertEquals("999ml", HydrationFormat.format(999))
    }

    @Test
    fun `a litre and up reads in litres to one decimal`() {
        assertEquals("1.0 L", HydrationFormat.format(1000))
        assertEquals("1.6 L", HydrationFormat.format(1600))
        assertEquals("2.4 L", HydrationFormat.format(2400))
        assertEquals("10.0 L", HydrationFormat.format(10_000))
    }

    @Test
    fun `cups are 250ml, one decimal, whole numbers plain`() {
        assertEquals("0 cups", HydrationFormat.format(0, HydrationUnit.CUPS))
        assertEquals("1 cup", HydrationFormat.format(250, HydrationUnit.CUPS))
        assertEquals("2 cups", HydrationFormat.format(500, HydrationUnit.CUPS))
        assertEquals("0.8 cups", HydrationFormat.format(200, HydrationUnit.CUPS))
        assertEquals("6.4 cups", HydrationFormat.format(1600, HydrationUnit.CUPS))
        assertEquals("9.6 cups", HydrationFormat.format(2400, HydrationUnit.CUPS))
    }
}
