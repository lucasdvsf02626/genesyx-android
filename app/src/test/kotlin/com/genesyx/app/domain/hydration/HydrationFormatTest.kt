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
}
