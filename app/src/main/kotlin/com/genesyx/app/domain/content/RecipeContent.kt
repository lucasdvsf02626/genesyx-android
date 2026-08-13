package com.genesyx.app.domain.content

import com.genesyx.app.domain.model.Nutrient
import com.genesyx.app.domain.model.Phase

/**
 * Recipe cards shown on the Nutrition tab. Content is intentionally **empty until it's supplied** —
 * like the Genesyx range catalogue, the section renders a "coming soon" state while [recipeContent]
 * has no rows, and fills in the moment recipes are added here (no other code change needed).
 *
 * Recipes are plain curated content (no backend, no cross-platform contract) — the same shape as
 * [nutritionPhaseFoods] and the Learn articles. When you paste content in, keep the health copy
 * within the app's guard-rails (no condition names, no dosing/medical claims — the banned-phrase
 * tests will catch these).
 */
data class Recipe(
    val id: String,
    val title: String,
    /** One-line hook shown under the title. */
    val subtitle: String,
    /** The phase this recipe best suits; null = good any time (always shown). */
    val phase: Phase? = null,
    val prepMinutes: Int? = null,
    val ingredients: List<String> = emptyList(),
    val steps: List<String> = emptyList(),
    val nutrients: List<Nutrient> = emptyList(),
)

/** The recipe library. Empty for now — add [Recipe] entries here when content is ready. */
val recipeContent: List<Recipe> = emptyList()

/**
 * Recipes to show for a given cycle phase: those tagged for that phase plus the always-shown general
 * ones. Before a cycle is set up ([phase] null) only the general recipes appear.
 */
fun recipesFor(phase: Phase?): List<Recipe> =
    recipeContent.filter { it.phase == null || it.phase == phase }
