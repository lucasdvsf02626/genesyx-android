package com.genesyx.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.genesyx.app.HiltTestActivity
import com.genesyx.app.data.SessionRepository
import com.genesyx.app.ui.components.GenesyxBottomNav
import com.genesyx.app.ui.navigation.GenesyxNavGraph
import com.genesyx.app.ui.navigation.Screen
import com.genesyx.app.ui.theme.GenesyxTheme
import com.genesyx.app.ui.track.detail.NUTRITION_EMPTY_ENTRY
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/**
 * Track → Nutrition must not be a dead-end summary: from the tracker she can log a supplement
 * by name (it lands in today's card at once) and open the same supplement-plan sheet the
 * Nutrition tab opens. Regression for the build-18 read-only tracker.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class TrackNutritionLoggingTest {

    @get:Rule(order = 0) val hilt = HiltAndroidRule(this)
    @get:Rule(order = 1) val compose = createAndroidComposeRule<HiltTestActivity>()

    @Inject lateinit var session: SessionRepository

    private lateinit var nav: NavHostController

    @Before
    fun setup() {
        hilt.inject()
        runBlocking {
            session.signIn(email = "track-log@example.com", name = "Track Logger", userId = "track-log-user")
            withTimeout(10_000) { while (!session.awaitSignedIn()) delay(50) }
        }
    }

    @Test
    fun track_nutrition_logs_a_supplement_by_name_and_opens_the_plan_sheet() {
        compose.setContent {
            val controller = rememberNavController()
            nav = controller
            GenesyxTheme(darkTheme = false) {
                Scaffold(bottomBar = { GenesyxBottomNav(controller) }) { padding ->
                    GenesyxNavGraph(controller, Screen.Home.route, Modifier.padding(padding))
                }
            }
        }
        compose.waitUntil(15_000) { hasAny("Track") && hasAny("Learn") }
        tapTab("Track")
        compose.waitUntil(8_000) { hasAny("Your trackers", "Add your cycle") }

        // The tracker row is a detail push (not a tab root).
        compose.onNode(hasContentDescription("Nutrition.", substring = true)).performScrollTo().performClick()
        // Eyebrows render uppercased, so wait on body copy rather than the "Log supplements" label.
        compose.waitUntil(8_000) { hasAny("Tick what you've taken today") }
        assertEquals(Screen.NutritionDetail.route, nav.currentDestination?.route)
        // Both TODAY cards start empty.
        assertEquals(2, compose.onAllNodesWithText(NUTRITION_EMPTY_ENTRY).fetchSemanticsNodes().size)

        // 1. A dedicated section, by name, with a checkbox per entry — the four essentials at least.
        listOf("Folate", "Omega-3", "Vitamin D", "Zinc").forEach { name ->
            compose.onNode(hasContentDescription("$name, ", substring = true)).assertExists()
        }

        // 2. Ticking Zinc logs it for today: the row flips and today's card names it.
        val zincBefore = compose.onNode(hasContentDescription("Zinc, not logged today")).performScrollTo()
        zincBefore.performClick()
        compose.waitUntil(8_000) {
            compose.onAllNodes(hasContentDescription("Zinc, logged today")).fetchSemanticsNodes().isNotEmpty()
        }
        // Today's supplements card now names it — only the food-groups card is still empty.
        compose.waitUntil(8_000) { compose.onAllNodesWithText(NUTRITION_EMPTY_ENTRY).fetchSemanticsNodes().size == 1 }
        compose.onNodeWithText("Supplements from today's log").performScrollTo().assertIsDisplayed()

        // 3. The plan sheet opens from here — the same one the Nutrition tab opens.
        compose.onNodeWithText("Supplement plan").performScrollTo().performClick()
        compose.waitUntil(8_000) { hasAny("Add your own supplement") }
        compose.onNodeWithText("Your supplement plan").assertIsDisplayed()
        compose.onNodeWithText("+ Add your own supplement").assertExists()
        compose.onNodeWithText("Got it").performClick()

        // 4. Un-log again — the clear persists as an empty row, never a stale tick.
        compose.onNode(hasContentDescription("Zinc, logged today")).performScrollTo().performClick()
        compose.waitUntil(8_000) {
            compose.onAllNodes(hasContentDescription("Zinc, not logged today")).fetchSemanticsNodes().isNotEmpty()
        }

        // Back returns to Track, with the tab highlighted (a detail push, not a tab root).
        compose.onNodeWithContentDescription("Back").performClick()
        compose.waitUntil(8_000) { hasAny("Your trackers", "Add your cycle") }
        assertEquals(Screen.Track.route, nav.currentDestination?.route)
    }

    private fun tapTab(label: String) {
        compose.waitUntil(8_000) { hasAny(label) }
        compose.onAllNodesWithText(label, substring = false, useUnmergedTree = true).onLast().performClick()
        compose.waitForIdle()
    }

    private fun hasAny(vararg needles: String): Boolean =
        needles.any { needle ->
            compose.onAllNodes(hasText(needle, substring = true), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
}
