package com.genesyx.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.espresso.Espresso
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
 * The Track → pH → Track regression, pinned against the real NavHost + bottom bar.
 *
 * The bug: `tracker/ph` is a bottom-tab root, but Track's "Vaginal pH" row and Home's nudge card
 * plain-pushed it. The next Track tab tap ran `popUpTo(Home) { saveState = true }` + `restoreState`,
 * saving and restoring the `track → tracker/ph` chain with pH on top — the pH screen stayed up,
 * the pH tab stayed highlighted, and Track looked dead (the SFM-27 mechanism, one route over).
 *
 * The fix routes every link that targets a tab through `navigateToTab`. These tests walk the
 * actual rows and cards and assert on the back stack, not just the visible screen: a push leaves
 * the previous tab's root underneath, a tab switch does not.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class PhTabNavigationTest {

    @get:Rule(order = 0) val hilt = HiltAndroidRule(this)
    @get:Rule(order = 1) val compose = createAndroidComposeRule<HiltTestActivity>()

    @Inject lateinit var session: SessionRepository

    private lateinit var nav: NavHostController

    @Before
    fun setup() {
        hilt.inject()
        // The dashboard sits behind an account; walk it the way she does, signed in.
        runBlocking {
            session.signIn(email = "phtab@example.com", name = "Ph Tab", userId = "phtab-user")
            withTimeout(10_000) { while (!session.awaitSignedIn()) delay(50) }
        }
    }

    @Test
    fun track_row_switches_to_the_ph_tab_and_the_track_tab_returns() {
        launchDashboard()
        tapTab("Track")
        compose.waitUntil(8_000) { hasAny("Your trackers", "Add your cycle") }

        // Track → pH must SWITCH to the pH tab. A plain push would leave `track` underneath.
        tapTrackerRow("Vaginal pH")
        compose.waitUntil(8_000) { nav.currentDestination?.route == Screen.PhDetail.route }
        assertEquals(0, backStackCount(Screen.Track.route))
        // pH is a tab root: no back arrow.
        compose.onAllNodesWithContentDescription("Back").assertCountEquals(0)

        // The regression itself: this tap used to restore the saved track→ph chain and look dead.
        tapTab("Track")
        compose.waitUntil(8_000) { nav.currentDestination?.route == Screen.Track.route }
        compose.waitUntil(8_000) { hasAny("Your trackers", "Add your cycle") }

        // Back and forth, plus a re-tap of the selected pH tab: never a second pH root.
        tapTab("pH")
        compose.waitUntil(8_000) { nav.currentDestination?.route == Screen.PhDetail.route }
        tapTab("pH")
        compose.waitUntil(8_000) { nav.currentDestination?.route == Screen.PhDetail.route }
        tapTab("Track")
        compose.waitUntil(8_000) { nav.currentDestination?.route == Screen.Track.route }
        tapTab("pH")
        compose.waitUntil(8_000) { nav.currentDestination?.route == Screen.PhDetail.route }
        assertEquals(1, backStackCount(Screen.PhDetail.route))

        // Track → pH → Home → Track: every tab tap lands on its own root.
        tapTab("Home")
        compose.waitUntil(8_000) { nav.currentDestination?.route == Screen.Home.route }
        tapTab("Track")
        compose.waitUntil(8_000) { nav.currentDestination?.route == Screen.Track.route }
        compose.waitUntil(8_000) { hasAny("Your trackers", "Add your cycle") }
    }
    @Test
    fun home_ph_card_switches_to_the_ph_tab_not_onto_track() {
        launchDashboard()
        compose.waitUntil(15_000) {
            compose.onAllNodes(hasContentDescription("Opens pH tracker", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }

        compose.onNode(hasContentDescription("Opens pH tracker", substring = true))
            .performScrollTo()
            .performClick()
        compose.waitUntil(8_000) { nav.currentDestination?.route == Screen.PhDetail.route }

        // A tab root: Home beneath, never Track (the old code pre-selected Track, then pushed pH).
        assertEquals(1, backStackCount(Screen.Home.route))
        assertEquals(0, backStackCount(Screen.Track.route))
        compose.onAllNodesWithContentDescription("Back").assertCountEquals(0)

        // And from there, Track works first time.
        tapTab("Track")
        compose.waitUntil(8_000) { nav.currentDestination?.route == Screen.Track.route }
        compose.waitUntil(8_000) { hasAny("Your trackers", "Add your cycle") }
    }

    @Test
    fun insights_ph_card_still_switches_to_the_ph_tab() {
        launchDashboard()
        tapTab("Insights")
        compose.waitUntil(8_000) { hasAny("Your Insights") }

        compose.onNodeWithText("Open tracker").performScrollTo().performClick()
        compose.waitUntil(8_000) { nav.currentDestination?.route == Screen.PhDetail.route }
        assertEquals(0, backStackCount(Screen.Insights.route))

        tapTab("Track")
        compose.waitUntil(8_000) { nav.currentDestination?.route == Screen.Track.route }
        compose.waitUntil(8_000) { hasAny("Your trackers", "Add your cycle") }
    }

    @Test
    fun the_other_tracker_rows_still_push_details_and_back_returns_to_track() {
        launchDashboard()
        tapTab("Track")
        compose.waitUntil(8_000) { hasAny("Your trackers", "Add your cycle") }

        // Only the pH row switches tabs. These five must keep opening as immersive details
        // stacked on Track, with a back arrow that returns to Track.
        listOf(
            "Cycle" to Screen.CycleDetail.route,
            "Nutrition" to Screen.NutritionDetail.route,
            "Symptoms" to Screen.SymptomsDetail.route,
            "Sleep" to Screen.SleepDetail.route,
            "Hydration" to Screen.HydrationDetail.route,
        ).forEach { (title, route) ->
            tapTrackerRow(title)
            compose.waitUntil(8_000) { nav.currentDestination?.route == route }
            // A pushed detail: Track's root is still underneath, and there is a Back affordance.
            assertEquals(1, backStackCount(Screen.Track.route))
            compose.onNodeWithContentDescription("Back").performClick()
            compose.waitUntil(8_000) { nav.currentDestination?.route == Screen.Track.route }
            compose.waitUntil(8_000) { hasAny("Your trackers", "Add your cycle") }
        }
    }

    @Test
    fun back_from_the_ph_tab_lands_on_home() {
        // Deliberate, and shared with Insights → pH and the article CTA: pH is a tab root whose
        // parent in the graph is Home, so Back from pH is a tab-to-Home pop, not a detail dismiss.
        launchDashboard()
        tapTab("Track")
        compose.waitUntil(8_000) { hasAny("Your trackers", "Add your cycle") }
        tapTrackerRow("Vaginal pH")
        compose.waitUntil(8_000) { nav.currentDestination?.route == Screen.PhDetail.route }

        Espresso.pressBack()
        compose.waitUntil(8_000) { nav.currentDestination?.route == Screen.Home.route }
        assertEquals(1, backStackCount(Screen.Home.route))
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private fun launchDashboard() {
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
    }

    private fun backStackCount(route: String): Int =
        nav.currentBackStack.value.count { it.destination.route == route }

    private fun tapTab(label: String) {
        compose.waitUntil(8_000) { hasAny(label) }
        compose.onAllNodesWithText(label, substring = false, useUnmergedTree = true)
            .onLast()
            .performClick()
        compose.waitForIdle()
    }

    /** A "Your trackers" row. Rows collapse to one semantics node: "{title}. {summary}". */
    private fun tapTrackerRow(title: String) {
        compose.waitUntil(8_000) {
            compose.onAllNodes(hasContentDescription("$title. ", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onAllNodes(hasContentDescription("$title. ", substring = true))
            .onFirst()
            .performScrollTo()
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

