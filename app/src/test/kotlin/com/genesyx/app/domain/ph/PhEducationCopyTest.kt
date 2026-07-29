package com.genesyx.app.domain.ph

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the four-question pH education copy and its sources.
 *
 * Two separate jobs here. The band-selection tests pin *which* copy each state gets — the failure
 * this prevents is an Elevated reading being shown the "nothing needs doing" text, which would be the
 * worst bug this screen could have. The citation tests pin that every claim-bearing section still
 * ships a reachable, dated source, because uncited health copy is a release blocker.
 *
 * Verbatim wording is not re-asserted here; [PhCopyBannedPhraseTest] scans it and the sections are
 * long enough that a duplicate copy of each string in the test would rot rather than protect.
 */
class PhEducationCopyTest {

    @Test
    fun `no readings yet gets the neutral copy, never a band verdict`() {
        assertEquals(PhCopy.MEANS_NONE, PhCopy.meansFor(null))
        assertEquals(PhCopy.DO_NONE, PhCopy.doNextFor(null))
    }

    @Test
    fun `a healthy reading gets healthy copy`() {
        assertEquals(PhCopy.MEANS_HEALTHY, PhCopy.meansFor(PhStatus.HEALTHY))
        assertEquals(PhCopy.DO_HEALTHY, PhCopy.doNextFor(PhStatus.HEALTHY))
    }

    @Test
    fun `an elevated reading gets elevated copy and is signposted to a clinician`() {
        assertEquals(PhCopy.MEANS_ELEVATED, PhCopy.meansFor(PhStatus.ELEVATED))
        assertEquals(PhCopy.DO_ELEVATED, PhCopy.doNextFor(PhStatus.ELEVATED))
        val signposted = listOf("GP", "nurse", "pharmacist", "sexual health clinic")
            .any { PhCopy.DO_ELEVATED.contains(it) }
        assertTrue("Elevated guidance must signpost a clinician", signposted)
    }

    @Test
    fun `healthy guidance does not signpost a clinician`() {
        // Sending every user to a GP for a normal reading is alarmist and would undermine the
        // signpost when it matters. The disclaimer already covers "if a reading worries you".
        assertTrue(!PhCopy.DO_HEALTHY.contains("GP"))
    }

    @Test
    fun `the supplements section makes no claim about changing ph`() {
        val text = PhCopy.SUPPLEMENTS_BODY.lowercase()
        assertTrue("Must explicitly disclaim a pH effect", text.contains("no claim"))
        listOf("improves", "restores", "corrects", "lowers your ph", "raises your ph").forEach {
            assertTrue("Supplement copy must not claim '$it'", !text.contains(it))
        }
    }

    @Test
    fun `every source is named, dated and reachable over https`() {
        assertTrue("The screen must cite at least two sources", PhCopy.SOURCES.size >= 2)
        PhCopy.SOURCES.forEach { c ->
            assertTrue("Citation needs a title", c.title.isNotBlank())
            assertTrue("Citation needs a publisher", c.publisher.isNotBlank())
            assertTrue("Citation needs a review date", c.reviewed.isNotBlank())
            assertTrue("Citation must be https: ${c.url}", c.url.startsWith("https://"))
        }
    }

    @Test
    fun `the clinical range in the copy matches the range the app enforces`() {
        // If PhStatus is ever retuned, this copy has to move with it — a screen that explains a
        // different range than the slider accepts is exactly the defect the client already reported.
        assertEquals(3.8, PhStatus.HEALTHY_MIN, 0.0)
        assertEquals(4.5, PhStatus.HEALTHY_MAX, 0.0)
        listOf(PhCopy.WHY_BODY, PhCopy.MEANS_HEALTHY, PhCopy.MEANS_ELEVATED).forEach {
            assertTrue("Copy must state the 3.8–4.5 band: $it", it.contains("3.8") && it.contains("4.5"))
        }
    }
}
