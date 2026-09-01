package com.genesyx.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
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
import com.genesyx.app.domain.content.AppGuide
import com.genesyx.app.ui.components.GenesyxBottomNav
import com.genesyx.app.ui.navigation.GenesyxNavGraph
import com.genesyx.app.ui.navigation.Screen
import com.genesyx.app.ui.theme.GenesyxTheme
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The "See your insights" dead button (reported 1 Sep 2026), pinned against the real NavHost.
 *
 * The article in the report — `reading-your-trends` — is the Insights tab's own "how this works"
 * link, so it is read with `insights → article` on the back stack. Its CTA navigated with
 * `popUpTo(Home) { saveState = true }` + `restoreState`, which saved that chain keyed by the tab
 * root at its bottom and then restored it in the same call: the article came back, nothing moved,
 * and the button looked broken. Same mechanism as SFM-27 and Track → pH → Track, one screen over.
 *
 * These tests assert on the back stack, not just the visible screen: the old code left the article
 * on it.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class ArticleCtaTabNavigationTest {

    @get:Rule(order = 0) val hilt = HiltAndroidRule(this)
    @get:Rule(order = 1) val compose = createAndroidComposeRule<HiltTestActivity>()

    @Inject lateinit var session: SessionRepository

    private lateinit var nav: NavHostController

    @Before
    fun setup() {
        hilt.inject()
        runBlocking {
            session.signIn(email = "cta@example.com", name = "Cta Tab", userId = "cta-user")
            withTimeout(10_000) { while (!session.awaitSignedIn()) delay(50) }
        }
    }

    @Test
    fun see_your_insights_leaves_the_article_it_was_opened_from() {
        launchDashboard()
        openInsightsGuideArticle()

        // The shape that broke: the article is pushed straight onto the Insights tab root.
        assertEquals(1, backStackCount(Screen.Insights.route))

        tapCta("See your insights")
        compose.waitUntil(8_000) { nav.currentDestination?.route == Screen.Insights.route }
        // Not merely "the route changed": the article must be off the stack, and the Insights root
        // we came from is the one we are back on.
        assertEquals(0, backStackCount(Screen.ArticleDetail.route))
        assertEquals(1, backStackCount(Screen.Insights.route))
        compose.waitUntil(8_000) { hasAny("Your Insights") }

        // And nothing was saved to spring back: the tab keeps working, article-free.
        tapTab("Learn")
        compose.waitUntil(8_000) { nav.currentDestination?.route == Screen.Learn.route }
        tapTab("Insights")
        compose.waitUntil(8_000) { nav.currentDestination?.route == Screen.Insights.route }
        assertEquals(0, backStackCount(Screen.ArticleDetail.route))
    }

    @Test
    fun the_same_cta_from_another_tab_switches_tabs_and_leaves_no_article_behind() {
        launchDashboard()
        tapTab("Learn")
        compose.waitUntil(8_000) { nav.currentDestination?.route == Screen.Learn.route }
        // Opened the way LearnScreen opens it: a plain push onto the Learn tab.
        openArticle(AppGuide.INSIGHTS)
        assertEquals(1, backStackCount(Screen.Learn.route))

        tapCta("See your insights")
        compose.waitUntil(8_000) { nav.currentDestination?.route == Screen.Insights.route }
        assertEquals(0, backStackCount(Screen.ArticleDetail.route))
        assertEquals(0, backStackCount(Screen.Learn.route))

        // Learn must come back as Learn — not as the bar-less article saved on top of it.
        tapTab("Learn")
        compose.waitUntil(8_000) { nav.currentDestination?.route == Screen.Learn.route }
        assertEquals(0, backStackCount(Screen.ArticleDetail.route))
    }

    @Test
    fun an_article_read_from_Home_is_not_restored_onto_the_Home_tab_later() {
        launchDashboard()
        // Home's cards push articles onto the Home root; the CTA then leaves for another tab.
        openArticle(AppGuide.INSIGHTS)
        assertEquals(1, backStackCount(Screen.Home.route))

        tapCta("See your insights")
        compose.waitUntil(8_000) { nav.currentDestination?.route == Screen.Insights.route }

        tapTab("Home")
        compose.waitUntil(8_000) { nav.currentDestination?.route == Screen.Home.route }
        assertEquals(0, backStackCount(Screen.ArticleDetail.route))
    }

    @Test
    fun back_from_the_article_still_returns_to_the_tab_that_opened_it() {
        // The pop is for the CTA only: Back is unchanged, and the Insights root is still beneath.
        launchDashboard()
        openInsightsGuideArticle()

        compose.onNodeWithContentDescription("Back").performClick()
        compose.waitUntil(8_000) { nav.currentDestination?.route == Screen.Insights.route }
        compose.waitUntil(8_000) { hasAny("Your Insights") }
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

    /** Insights tab → its own "how this works" link: the path in the bug report. */
    private fun openInsightsGuideArticle() {
        tapTab("Insights")
        compose.waitUntil(8_000) { hasAny("Your Insights") }
        compose.onNodeWithText(AppGuide.INSIGHTS_LABEL).performScrollTo().performClick()
        compose.waitUntil(8_000) { nav.currentDestination?.route == Screen.ArticleDetail.route }
        compose.waitForIdle()
    }

    /** A plain push, exactly as every list and card in the app opens an article. */
    private fun openArticle(slug: String) {
        compose.runOnUiThread { nav.navigate(Screen.ArticleDetail.create(slug)) }
        compose.waitUntil(8_000) { nav.currentDestination?.route == Screen.ArticleDetail.route }
        compose.waitForIdle()
    }

    private fun tapCta(label: String) {
        compose.waitUntil(8_000) { hasAny(label) }
        compose.onNodeWithText(label).performScrollTo().performClick()
        compose.waitForIdle()
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

    private fun hasAny(vararg needles: String): Boolean =
        needles.any { needle ->
            compose.onAllNodes(hasText(needle, substring = true), useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
}
