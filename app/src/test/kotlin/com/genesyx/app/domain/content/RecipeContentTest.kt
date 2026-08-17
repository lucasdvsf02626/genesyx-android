package com.genesyx.app.domain.content

import com.genesyx.app.domain.model.Phase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeContentTest {

    @Test
    fun `every recipe names a focus food that exists in its own phase`() {
        recipeContent.forEach { recipe ->
            val phase = requireNotNull(recipe.phase) { "${recipe.title} has no phase" }
            val reviewed = nutritionPhaseFoods.getValue(phase).map { it.name }
            assertTrue(
                "\"${recipe.title}\" claims focus food \"${recipe.usesFocusFood}\", " +
                    "which is not in the reviewed $phase list $reviewed",
                reviewed.contains(recipe.usesFocusFood),
            )
        }
    }

    @Test
    fun `recipe copy makes no health claim`() {
        val claimWords = listOf(
            "boost", "improve", "helps you", "help you", "good for", "supports", "support your",
            "increases", "reduces", "prevents", "treats", "cures", "balances", "optimis",
            "fertility", "conceive", "egg quality", "hormone", "detox", "cleanse",
        )
        val surfaces = recipeContent.flatMap { recipe ->
            listOf(recipe.title, recipe.subtitle) + recipe.ingredients + recipe.steps
        } + listOf(RecipeCopy.title, RecipeCopy.eyebrow, RecipeCopy.footnote, RecipeCopy.comingSoon)
        surfaces.forEach { text ->
            val lower = text.lowercase()
            claimWords.forEach { word ->
                assertFalse("\"$word\" in recipe copy: $text", lower.contains(word))
            }
        }
    }

    @Test
    fun `every phase has two uniquely named recipes with photography`() {
        assertEquals(8, recipeContent.size)
        assertEquals(recipeContent.size, recipeContent.map { it.title }.toSet().size)
        Phase.entries.forEach { phase ->
            val forPhase = recipesFor(phase)
            assertEquals("$phase should have two recipes", 2, forPhase.size)
        }
        recipeContent.forEach { recipe ->
            assertTrue(recipe.title.isNotBlank())
            assertTrue("${recipe.title} needs a photo", recipe.imageRes != null && recipe.imageRes != 0)
            assertTrue("${recipe.title} takes no time", (recipe.prepMinutes ?: 0) > 0)
            assertTrue("${recipe.title} serves nobody", (recipe.serves ?: 0) > 0)
            assertTrue("${recipe.title} is short on ingredients", recipe.ingredients.size >= 3)
            assertTrue("${recipe.title} is short on method", recipe.steps.size >= 2)
            assertTrue("${recipe.title} names no food groups", recipe.groups.isNotEmpty())
            assertEquals(
                "${recipe.title} names a group twice",
                recipe.groups.toSet().size,
                recipe.groups.size,
            )
        }
        assertEquals(
            "two recipes point at the same photograph",
            recipeContent.size,
            recipeContent.map { it.imageRes }.toSet().size,
        )
    }

    @Test
    fun `log groups action is additive wording not a score`() {
        assertEquals("Log vegetables", RecipeCopy.logGroupsAction(listOf("vegetables")))
        assertEquals(
            "Log vegetables and protein",
            RecipeCopy.logGroupsAction(listOf("vegetables", "protein")),
        )
        assertFalse(RecipeCopy.logGroupsAction(listOf("vegetables", "protein")).contains("complete"))
    }

    @Test
    fun `unknown phase shows the whole library so the section is not empty`() {
        assertEquals(recipeContent, recipesFor(null))
    }
}
