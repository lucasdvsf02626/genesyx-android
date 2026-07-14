package com.genesyx.app.ui.components

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.genesyx.app.domain.model.CycleSettings
import com.genesyx.app.ui.theme.GenesyxTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * The dialog used to prefill `lastPeriodDate = today`, so a user who opened it and tapped Save
 * without touching the date silently committed "my period started today" — and Home then rendered
 * "DAY 1 · PERIOD", a fertile window and a month of predictions from a value she never entered.
 *
 * Every cycle claim in the app derives from this one field, so these pin that it can only ever come
 * from the user.
 */
@RunWith(AndroidJUnit4::class)
class CycleSettingsDialogTest {

    @get:Rule val compose = createComposeRule()

    @Test
    fun an_untouched_dialog_cannot_save() {
        var saved: CycleSettings? = null
        compose.setContent {
            GenesyxTheme(darkTheme = false) {
                CycleSettingsDialog(current = null, onDismiss = {}, onSave = { saved = it })
            }
        }

        // No date chosen: the field is a placeholder and Save is inert.
        compose.onNodeWithText("Select date").assertExists()
        compose.onNodeWithText("Save").assertIsNotEnabled()

        compose.onNodeWithText("Save").performClick()

        assertNull("an untouched dialog must not commit a date", saved)
    }

    @Test
    fun an_existing_date_is_shown_and_saveable() {
        val existing = CycleSettings(LocalDate.of(2026, 7, 1), cycleLength = 30, periodLength = 4)
        var saved: CycleSettings? = null
        compose.setContent {
            GenesyxTheme(darkTheme = false) {
                CycleSettingsDialog(current = existing, onDismiss = {}, onSave = { saved = it })
            }
        }

        // Editing an existing cycle is unaffected — her real date is already there.
        compose.onNodeWithText("Select date").assertDoesNotExist()
        compose.onNodeWithText("Save").assertIsEnabled()

        compose.onNodeWithText("Save").performClick()

        assertEquals(existing, saved)
    }
}
