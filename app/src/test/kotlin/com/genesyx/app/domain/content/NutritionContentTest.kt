package com.genesyx.app.domain.content

import com.genesyx.app.domain.model.Phase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Completeness + integrity checks for the ported nutrition/cycle content and overlay helpers. */
class NutritionContentTest {

    // ── Nutrition focus foods (screens/Nutrition.tsx PHASE_FOODS) ──

    @Test
    fun `every phase has focus foods with the expected counts`() {
        assertEquals(Phase.entries.toSet(), nutritionPhaseFoods.keys)
        assertEquals(3, nutritionPhaseFoods.getValue(Phase.PERIOD).size)
        assertEquals(3, nutritionPhaseFoods.getValue(Phase.FOLLICULAR).size)
        assertEquals(4, nutritionPhaseFoods.getValue(Phase.OVULATORY).size)
        assertEquals(4, nutritionPhaseFoods.getValue(Phase.LUTEAL).size)
    }

    @Test
    fun `every focus food has non-blank copy and a richer expanded description`() {
        nutritionPhaseFoods.values.flatten().forEach { food ->
            assertTrue("name blank", food.name.isNotBlank())
            assertTrue("shortDesc blank for ${food.name}", food.shortDesc.isNotBlank())
            assertTrue("expandedDesc blank for ${food.name}", food.expandedDesc.isNotBlank())
            assertTrue(
                "expandedDesc should be longer than shortDesc for ${food.name}",
                food.expandedDesc.length > food.shortDesc.length,
            )
        }
    }

    @Test
    fun `each phase uses one accent and the four phase accents are distinct`() {
        nutritionPhaseFoods.forEach { (phase, foods) ->
            val accents = foods.map { it.accent }.toSet()
            assertEquals("phase $phase should use a single accent", 1, accents.size)
        }
        val phaseAccents = nutritionPhaseFoods.values.map { it.first().accent }.toSet()
        assertEquals("the four phases should have distinct accents", 4, phaseAccents.size)
    }

    @Test
    fun `phase descriptions cover every phase and are non-blank`() {
        assertEquals(Phase.entries.toSet(), nutritionPhaseDescription.keys)
        nutritionPhaseDescription.values.forEach { assertTrue(it.isNotBlank()) }
    }

    @Test
    fun `supplement plan is folate omega-3 vitamin D and zinc`() {
        assertEquals(listOf("F", "O", "D", "Z"), supplementPlan.map { it.initial })
        supplementPlan.forEach {
            assertTrue(it.name.isNotBlank())
            assertTrue(it.rationale.isNotBlank())
        }
    }

    @Test
    fun `there are three learn-more articles with copy`() {
        assertEquals(3, nutritionArticles.size)
        nutritionArticles.forEach {
            assertTrue(it.title.isNotBlank())
            assertTrue(it.read.isNotBlank())
        }
    }

    // ── Cycle content (lib/cycle.ts phaseHeroCopy / phaseFoods / phaseLabel) ──

    @Test
    fun `cycle content covers every phase`() {
        assertEquals(Phase.entries.toSet(), phaseHeroCopy.keys)
        assertEquals(Phase.entries.toSet(), phaseLabel.keys)
        assertEquals(Phase.entries.toSet(), phaseFoods.keys)
        phaseFoods.values.forEach { assertEquals(4, it.size) }
        phaseHeroCopy.values.forEach {
            assertTrue(it.hero.isNotBlank())
            assertTrue(it.sub.isNotBlank())
            assertTrue(it.tags.isNotEmpty())
            assertTrue(it.focus.title.isNotBlank())
            assertTrue(it.focus.body.isNotBlank())
        }
    }

    // ── Fertile-window overlay (lib/cycleEngine.ts) ──

    @Test
    fun `sub-label is the phase label normally and 'Fertile window' when fertile`() {
        Phase.entries.forEach { phase ->
            assertEquals(phaseLabel.getValue(phase), phaseSubLabel(phase, inFertile = false))
            assertEquals("Fertile window", phaseSubLabel(phase, inFertile = true))
        }
    }

    @Test
    fun `hero text overlays for non-ovulatory fertile days but not ovulation itself`() {
        // Ovulatory keeps its own hero even inside the fertile window.
        assertEquals(phaseHeroCopy.getValue(Phase.OVULATORY).hero, phaseHeroText(Phase.OVULATORY, true))
        // Other phases switch to the fertile-window hero when fertile.
        assertEquals("Fertile window is open", phaseHeroText(Phase.FOLLICULAR, true))
        assertEquals(phaseHeroCopy.getValue(Phase.FOLLICULAR).hero, phaseHeroText(Phase.FOLLICULAR, false))
        assertTrue(phaseHeroSubtext(Phase.FOLLICULAR, true).contains("Conception"))
        assertEquals(phaseHeroCopy.getValue(Phase.LUTEAL).sub, phaseHeroSubtext(Phase.LUTEAL, false))
    }

    @Test
    fun `tags prepend 'Fertile window' for non-ovulatory fertile days only`() {
        val base = phaseHeroCopy.getValue(Phase.FOLLICULAR).tags
        val fertile = phaseTags(Phase.FOLLICULAR, inFertile = true)
        assertEquals("Fertile window", fertile.first())
        assertEquals(base.size + 1, fertile.size)
        // Ovulatory is not overlaid.
        assertEquals(phaseHeroCopy.getValue(Phase.OVULATORY).tags, phaseTags(Phase.OVULATORY, true))
        // Not fertile -> unchanged.
        assertEquals(base, phaseTags(Phase.FOLLICULAR, false))
    }
}
