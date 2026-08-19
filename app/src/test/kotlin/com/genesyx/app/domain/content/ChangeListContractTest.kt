package com.genesyx.app.domain.content

import com.genesyx.app.core.AppLinks
import com.genesyx.app.core.FeatureFlags
import com.genesyx.app.domain.hydration.HydrationCoach
import com.genesyx.app.domain.model.DailyLog
import com.genesyx.app.domain.model.isMeaningful
import com.genesyx.app.domain.ph.PhCopy
import com.genesyx.app.ui.navigation.Screen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the client change list that this pass was asked to implement or verify.
 * A "Done" in the progress report must have a failing test if someone undoes it.
 */
class ChangeListContractTest {

    @Test
    fun `1A customer pH copy is vaginal never urine pH`() {
        val all = PhCopy.all().joinToString("\n").lowercase()
        assertFalse(all.contains("urine ph"))
        assertTrue(all.contains("vaginal ph"))
        assertEquals("urine (legacy)", PhCopy.LEGACY_MARKER)
    }

    @Test
    fun `1A pH is a bottom tab and is not a Nutrition destination`() {
        assertTrue(Screen.bottomTabs.contains(Screen.PhDetail))
        assertEquals("tracker/ph", Screen.PhDetail.route)
        assertFalse(Screen.Nutrition.route.contains("ph"))
    }

    @Test
    fun `1A science and shettles websites stay hidden until they are real pages`() {
        assertFalse(AppLinks.isConfiguredWebPage(AppLinks.SCIENCE_URL))
        assertFalse(AppLinks.isConfiguredWebPage(AppLinks.SHETTLES_THEORY_URL))
        assertTrue(PhCopy.SHETTLES_BODY.lowercase().contains("not a proven method"))
    }

    @Test
    fun `1C sex preference is optional and names the four allowed answers`() {
        val gender = quizQuestions.first { it.id == GENDER_QUESTION_ID }
        assertTrue(gender.optional)
        assertTrue(gender.canContinue(null))
        assertEquals(
            listOf("Girl", "Boy", "No preference", "Prefer not to say"),
            gender.options.map { it.label },
        )
        assertFalse(gender.helper.lowercase().contains("guarantee"))
    }

    @Test
    fun `1B intimacy never qualifies a streak day`() {
        assertFalse(DailyLog(sexualActivity = true).isMeaningful())
    }

    @Test
    fun `2B hydration why-expander copy is ready to hide the grey block`() {
        assertEquals("Why hydration?", HydrationCoach.WHY_TITLE)
        assertTrue(HydrationCoach.WHY_TEXT.contains("eight glasses"))
    }

    @Test
    fun `4 partner and admin stay off`() {
        assertFalse(FeatureFlags.PARTNER_INVITES)
        assertFalse(FeatureFlags.ADMIN_CLIENTS)
        assertTrue(FeatureFlags.PH_TRACKING)
    }
}
