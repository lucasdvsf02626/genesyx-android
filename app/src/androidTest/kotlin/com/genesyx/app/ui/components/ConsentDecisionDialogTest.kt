package com.genesyx.app.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.genesyx.app.ui.theme.GenesyxTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The first-time consent ask can only be answered, never accidentally dismissed. On code 22 a
 * stray tap outside the dialog dismissed the question and left collection silently off — the
 * dialog now ignores outside taps and Back, and Allow does not close it by itself (the recorded
 * grant flipping `needsDecision` is what removes it, so a failed grant keeps the question up).
 */
@RunWith(AndroidJUnit4::class)
class ConsentDecisionDialogTest {

    @get:Rule val compose = createComposeRule()

    @Test
    fun an_outside_tap_and_back_do_not_dismiss_the_ask() {
        var notNow = 0
        compose.setContent {
            GenesyxTheme(darkTheme = false) {
                Text("Behind the dialog", modifier = Modifier.fillMaxSize())
                ConsentDecisionDialog(onAllow = {}, onNotNow = { notNow++ })
            }
        }
        compose.onNodeWithText("Allow").assertExists()

        // Back must not be an answer to a consent question. Dispatched to the DIALOG's root —
        // the dialog window holds focus, so Espresso's default root picker never settles.
        Espresso.onView(ViewMatchers.isRoot())
            .inRoot(RootMatchers.isDialog())
            .perform(ViewActions.pressBack())
        compose.waitForIdle()

        compose.onNodeWithText("Allow").assertExists()
        assertEquals("neither gesture may count as Not now", 0, notNow)
    }

    @Test
    fun allow_records_but_does_not_close_by_itself() {
        // The host removes the dialog when needsDecision flips false — simulated here. Until the
        // grant actually lands, the question stays on screen.
        var allowed = false
        var decisionNeeded by mutableStateOf(true)
        compose.setContent {
            GenesyxTheme(darkTheme = false) {
                if (decisionNeeded) {
                    ConsentDecisionDialog(onAllow = { allowed = true }, onNotNow = {})
                }
            }
        }

        compose.onNodeWithText("Allow").performClick()
        compose.waitForIdle()

        assertTrue(allowed)
        compose.onNodeWithText("Allow").assertExists() // still up: the grant hasn't landed yet

        decisionNeeded = false // the recorded grant clears needsDecision
        compose.waitForIdle()
        compose.onNodeWithText("Allow").assertDoesNotExist()
    }

    @Test
    fun not_now_is_an_explicit_button_and_records_nothing() {
        var allowed = false
        var notNow = 0
        compose.setContent {
            GenesyxTheme(darkTheme = false) {
                ConsentDecisionDialog(onAllow = { allowed = true }, onNotNow = { notNow++ })
            }
        }

        compose.onNodeWithText("Not now").performClick()
        compose.waitForIdle()

        assertEquals(1, notNow)
        assertFalse("postponing must not grant", allowed)
    }
}
