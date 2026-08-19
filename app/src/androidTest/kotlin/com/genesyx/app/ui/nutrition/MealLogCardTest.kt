package com.genesyx.app.ui.nutrition

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.genesyx.app.domain.model.MealEntry
import com.genesyx.app.domain.model.MealType
import com.genesyx.app.domain.model.Nutrient
import com.genesyx.app.ui.theme.GenesyxTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.LocalDateTime

@RunWith(AndroidJUnit4::class)
class MealLogCardTest {

    @get:Rule val compose = createComposeRule()

    private val meal = MealEntry(
        id = "meal-1",
        date = LocalDate.of(2026, 8, 12),
        type = MealType.LUNCH,
        description = "Lentil salad",
        nutrients = listOf(Nutrient.IRON),
        loggedAt = LocalDateTime.of(2026, 8, 12, 13, 30),
    )

    @Test
    fun empty_state_invites_a_first_meal() {
        compose.setContent {
            GenesyxTheme(darkTheme = false) {
                MealLogCard(meals = emptyList(), onLog = {}, onDelete = {})
            }
        }
        compose.onNodeWithText(
            "Optional. The chips above are the record; add a description here if you want one. Kept on this device.",
        ).assertExists()
        compose.onNodeWithText("Log a meal").assertIsEnabled()
    }

    @Test
    fun tapping_a_meal_opens_the_edit_dialog_prefilled_and_keeps_its_id() {
        var saved: MealEntry? = null
        compose.setContent {
            GenesyxTheme(darkTheme = false) {
                MealLogCard(meals = listOf(meal), onLog = { saved = it }, onDelete = {})
            }
        }

        // Tapping the row opens the EDIT dialog (not the add flow) with the description prefilled.
        compose.onNodeWithText("Lentil salad").performClick()
        compose.onNodeWithText("Edit meal").assertExists()
        compose.onNodeWithText("Save").assertIsEnabled().performClick()

        // A round-trip save preserves identity — same id/date, unchanged fields.
        assertEquals("meal-1", saved?.id)
        assertEquals(LocalDate.of(2026, 8, 12), saved?.date)
        assertEquals(MealType.LUNCH, saved?.type)
        assertEquals("Lentil salad", saved?.description)
        assertEquals(listOf(Nutrient.IRON), saved?.nutrients)
    }

    @Test
    fun the_add_button_opens_the_log_a_meal_dialog() {
        compose.setContent {
            GenesyxTheme(darkTheme = false) {
                MealLogCard(meals = emptyList(), onLog = {}, onDelete = {})
            }
        }
        compose.onNodeWithText("Log a meal").performClick()
        // The dialog title ("Log a meal") now also exists — Save is inert until a description is typed.
        compose.onNodeWithText("What did you eat?").assertExists()
    }
}
