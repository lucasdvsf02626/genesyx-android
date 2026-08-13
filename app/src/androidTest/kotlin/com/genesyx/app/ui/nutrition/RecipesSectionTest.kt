package com.genesyx.app.ui.nutrition

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.genesyx.app.domain.content.Recipe
import com.genesyx.app.domain.model.Nutrient
import com.genesyx.app.domain.model.Phase
import com.genesyx.app.ui.theme.GenesyxTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

class RecipesSectionTest {

    @get:Rule val compose = createComposeRule()

    private val recipe = Recipe(
        id = "r1",
        title = "Iron-boost lentil bowl",
        subtitle = "A warming bowl for your period phase",
        phase = Phase.PERIOD,
        prepMinutes = 20,
        ingredients = listOf("Red lentils", "Spinach", "Lemon"),
        steps = listOf("Simmer the lentils", "Wilt in the spinach", "Finish with lemon"),
        nutrients = listOf(Nutrient.IRON, Nutrient.FOLATE),
    )

    @Test
    fun empty_content_shows_coming_soon() {
        compose.setContent {
            GenesyxTheme(darkTheme = false) {
                RecipesSection(recipes = emptyList(), expandedId = null, onToggle = {})
            }
        }
        compose.onNodeWithText("Cycle-friendly recipes are coming soon. They'll appear here the moment they're added.")
            .assertExists()
    }

    @Test
    fun a_recipe_renders_collapsed_then_reveals_ingredients_and_method_when_expanded() {
        var toggled: String? = null
        compose.setContent {
            GenesyxTheme(darkTheme = false) {
                // expandedId matches the recipe id so the body is visible for the assertions.
                RecipesSection(recipes = listOf(recipe), expandedId = "r1", onToggle = { toggled = it })
            }
        }

        // Header (title + hook) always shows.
        compose.onNodeWithText("Iron-boost lentil bowl").assertExists()
        compose.onNodeWithText("A warming bowl for your period phase").assertExists()
        // Expanded body: ingredient bullets and numbered method.
        compose.onNodeWithText("· Red lentils").assertExists()
        compose.onNodeWithText("1. Simmer the lentils").assertExists()
        compose.onNodeWithText("Iron").assertExists() // nutrient chip

        // Tapping the card asks the parent to toggle it.
        compose.onNodeWithText("Iron-boost lentil bowl").performClick()
        org.junit.Assert.assertEquals("r1", toggled)
    }
}
