package com.genesyx.app.domain.ph

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pins the client-signed-off vaginal-pH copy to its exact wording. The expected strings are
 * hardcoded here (NOT read from [PhCopy]) so that an accidental edit to a constant — a changed word,
 * a dropped apostrophe, altered punctuation — fails the build instead of silently shipping. If copy
 * genuinely changes, update both [PhCopy] and this test deliberately.
 */
class PhCopyTest {

    @Test
    fun `healthy insight copy is verbatim`() {
        assertEquals(
            "Your recent readings sit within the typical healthy range.",
            PhCopy.INSIGHT_HEALTHY,
        )
    }

    @Test
    fun `elevated insight copy is verbatim`() {
        assertEquals(
            "Your recent readings are above the typical healthy range.",
            PhCopy.INSIGHT_ELEVATED,
        )
    }

    @Test
    fun `elevated signpost copy is verbatim`() {
        assertEquals(
            "If readings stay above the usual range over several days, a GP or pharmacist can talk it " +
                "through with you.",
            PhCopy.RECOMMENDATION_ELEVATED,
        )
    }

    @Test
    fun `disclaimer copy is verbatim`() {
        assertEquals(
            "This tracker is for your own record and isn't medical advice. If a reading worries you, " +
                "or a pattern persists, please speak to a GP, nurse, or pharmacist.",
            PhCopy.DISCLAIMER,
        )
    }

    @Test
    fun `one-time notice copy is verbatim`() {
        assertEquals(
            "This tracker now records vaginal pH. Any readings you logged before this update are kept " +
                "and marked 'urine (legacy)'. New readings are saved as vaginal pH, on a different scale.",
            PhCopy.NOTICE_BODY,
        )
    }

    @Test
    fun `legacy marker is the one canonical lowercase string`() {
        assertEquals("urine (legacy)", PhCopy.LEGACY_MARKER)
    }
}
