package com.genesyx.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
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
 * SFM-27, reproduced and pinned against the real NavHost + bottom bar.
 *
 * The bug: Nutrition's "See all articles" pushed the Learn root on top of Nutrition. The next tab
 * switch saved that chain under Nutrition's destination, and every later Nutrition tap restored
 * it with Learn on top — the tab looked dead. This walks exactly that path and asserts the tab
 * lands on Nutrition, as a root, with no duplicate entry on a re-tap.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class NutritionTabNavigationTest {

    @get:Rule(order = 0) val hilt = HiltAndroidRule(this)
    @get:Rule(order = 1) val compose = createAndroidComposeRule<HiltTestActivity>()

    @Inject lateinit var session: SessionRepository

    private lateinit var nav: NavHostController

    @Before
    fun setup() {
        hilt.inject()
        // The dashboard sits behind an account; walk it the way she does, signed in.
        runBlocking {
            session.signIn(email = "sfm27@example.com", name = "Sfm TwentySeven", userId = "sfm27-user")
            withTimeout(10_000) { while (!session.awaitSignedIn()) delay(50) }
        }
    }

    @Test
    fun nutrition_tab_is_registered_navigable_and_survives_its_own_cross_tab_link() {
        compose.setContent {
            val controller = rememberNavController()
            nav = controller
            GenesyxTheme(darkTheme = false) {
                Scaffold(bottomBar = { GenesyxBottomNav(controller) }) { padding ->
                    GenesyxNavGraph(
                        navController = controller,
                        startDestination = Screen.Home.route,
                        modifier = Modifier.padding(padding),
                    )
                }
            }
        }
        compose.waitUntil(15_000) { hasAny("Nutrition") && hasAny("Learn") }

        // 1. The route is registered and the tab reaches it, as a root (no Back).
        tapTab("Nutrition")
        compose.waitUntil(8_000) { hasAny("Your nutrition focus") }
        assertEquals(Screen.Nutrition.route, nav.currentDestination?.route)
        compose.onAllNodesWithContentDescription("Back").assertCountEquals(0)

        // 2. The link that used to break it: Nutrition → Learn (another tab root).
        compose.onNodeWithText("See all articles").performScrollTo().performClick()
        compose.waitUntil(8_000) { nav.currentDestination?.route == Screen.Learn.route }
        compose.waitUntil(8_000) { hasAny("Short reads", "How to use Genesyx") }

        // 3. Away to a third tab, then back to Nutrition — this is where it used to show Learn.
        tapTab("Track")
        compose.waitUntil(8_000) { hasAny("Your trackers", "Add your cycle") }
        tapTab("Nutrition")
        compose.waitUntil(8_000) { hasAny("Your nutrition focus") }
        assertEquals(Screen.Nutrition.route, nav.currentDestination?.route)
        // restoreState also restores the scroll position (we were at the bottom), so scroll first.
        compose.onNodeWithText("Your nutrition focus").performScrollTo().assertIsDisplayed()
        compose.onAllNodesWithContentDescription("Back").assertCountEquals(0)

        // 4. Re-tapping the selected tab never stacks a second Nutrition.
        tapTab("Nutrition")
        assertEquals(Screen.Nutrition.route, nav.currentDestination?.route)
        assertEquals(1, nav.currentBackStack.value.count { it.destination.route == Screen.Nutrition.route })
    }

    private fun tapTab(label: String) {
        compose.waitUntil(8_000) { hasAny(label) }
        compose.onAllNodesWithText(label, substring = false, useUnmergedTree = true)
            .onLast()
            .performClick()
        compose.waitForIdle()
    }

    private fun hasAny(vararg needles: String): Boolean =
        needles.any { needle ->
            compose.onAllNodes(hasText(needle, substring = true), useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
}
