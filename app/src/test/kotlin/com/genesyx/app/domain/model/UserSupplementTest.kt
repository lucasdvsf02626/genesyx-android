package com.genesyx.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UserSupplementTest {

    @Test
    fun `time-of-day wire values match the server CHECK constraint exactly`() {
        // Cross-platform contract (user_supplements.time_of_day) — renaming one breaks sync.
        assertEquals(
            listOf("morning", "afternoon", "evening", "anytime"),
            SupplementTime.entries.map { it.wire },
        )
    }

    @Test
    fun `fromWire round-trips every value and rejects unknowns without crashing`() {
        SupplementTime.entries.forEach { assertEquals(it, SupplementTime.fromWire(it.wire)) }
        assertNull(SupplementTime.fromWire("noon"))
        assertNull(SupplementTime.fromWire(null))
    }

    @Test
    fun `name limit matches the server contract`() {
        assertEquals(60, UserSupplement.NAME_MAX_LENGTH)
    }
}
