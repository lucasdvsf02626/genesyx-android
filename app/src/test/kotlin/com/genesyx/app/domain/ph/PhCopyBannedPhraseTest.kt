package com.genesyx.app.domain.ph

import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * Release blocker, mirroring the Learn banned-phrase guard: none of the vaginal-pH copy may name a
 * condition, imply treatment/cure, or offer a diagnosis. The banned list is kept in step with
 * `LearnContentTest`'s (same terms, extended for this migration).
 */
class PhCopyBannedPhraseTest {

    private val banned = listOf(
        "boy or girl", "sex-selection", "sway", "alkaline diet", "alkaline water",
        "douch", "optimize your ph", "balance your ph", "conceiving a boy", "conceiving a girl",
        "bacterial vaginosis", "bv", "infection", "thrush", "candida", "yeast", "treat", "cure", "diagnos",
        // No dietary advice in pH copy (audit 22 Jul 2026).
        "leafy greens", "whole grains", "mineral water",
    )

    @Test
    fun `no banned health claims appear in any ph copy`() {
        PhCopy.all().forEach { copy ->
            val text = copy.lowercase()
            banned.forEach { phrase ->
                assertFalse("pH copy contains banned phrase '$phrase': \"$copy\"", text.contains(phrase))
            }
        }
    }
}
