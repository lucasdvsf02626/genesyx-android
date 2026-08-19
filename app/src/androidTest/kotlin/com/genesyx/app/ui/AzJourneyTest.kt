package com.genesyx.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.genesyx.app.HiltTestActivity
import com.genesyx.app.data.SessionRepository
import com.genesyx.app.domain.content.FreeGuideContent
import com.genesyx.app.ui.components.GenesyxBottomNav
import com.genesyx.app.ui.navigation.GenesyxNavGraph
import com.genesyx.app.ui.navigation.Screen
import com.genesyx.app.ui.onboarding.SplashScreen
import com.genesyx.app.ui.screens.AuthScreen
import com.genesyx.app.ui.theme.GenesyxTheme
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/**
 * A–Z walk of the signed-in shell and the unsigned gate, hosted on [HiltTestActivity] so
 * MainActivity / Credential Manager / BootReceiver cannot steal the process.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class AzJourneyTest {

    @get:Rule(order = 0) val hilt = HiltAndroidRule(this)
    @get:Rule(order = 1) val compose = createAndroidComposeRule<HiltTestActivity>()

    @Inject lateinit var session: SessionRepository

    @Before
    fun setup() {
        hilt.inject()
    }

    @Test
    fun unsigned_splash_opens_the_auth_gate() {
        compose.setContent {
            var openedAuth by remember { mutableStateOf(false) }
            GenesyxTheme(darkTheme = false) {
                if (!openedAuth) {
                    SplashScreen(onStart = {}, onSignIn = { openedAuth = true })
                } else {
                    AuthScreen(onSignedIn = {}, onBack = { openedAuth = false })
                }
            }
        }
        compose.onNodeWithText("Start Your Personalised Quiz").assertIsDisplayed()
        compose.onNodeWithText("Sign in").performClick()
        compose.waitUntil(8_000) { hasAny("Welcome back") }
        compose.onNodeWithText("Welcome back").assertIsDisplayed()
        compose.onNodeWithText("Create account").assertIsDisplayed()
        compose.onNodeWithText("Continue with Google").assertIsDisplayed()
    }

    @Test
    fun signed_in_walks_every_tab_and_the_learn_hub() {
        becomeSignedIn()
        compose.setContent {
            val nav = rememberNavController()
            GenesyxTheme(darkTheme = false) {
                Scaffold(bottomBar = { GenesyxBottomNav(nav) }) { padding ->
                    GenesyxNavGraph(
                        navController = nav,
                        startDestination = Screen.Home.route,
                        modifier = Modifier.padding(padding),
                    )
                }
            }
        }
        compose.waitUntil(15_000) { hasAny("Welcome to Genesyx", "Home") && hasAny("Track") && hasAny("Learn") }

        tapTab("Track")
        compose.waitUntil(8_000) { hasAny("Your trackers", "Add your cycle") }

        tapTab("pH")
        compose.waitUntil(8_000) { hasAny("Vaginal pH", "Healthy") }

        tapTab("Nutrition")
        compose.waitUntil(8_000) { hasAny("Log a meal", "coming soon", "Recipes") }

        tapTab("Insights")
        compose.waitUntil(8_000) { hasAny("Insights", "this week", "Weekly") }

        tapTab("Learn")
        compose.waitUntil(8_000) { hasAny("Short reads", "How to use Genesyx") }
        compose.onNodeWithText("How to use Genesyx").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Start your 12-week plan here").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("7-day nutrition starter guide").performScrollTo().performClick()
        compose.waitUntil(8_000) { hasAny(FreeGuideContent.title) }
        compose.onNodeWithContentDescription("Back").performClick()

        compose.waitUntil(8_000) { hasAny("Start your 12-week plan here") }
        compose.onNodeWithText("Start your 12-week plan here").performScrollTo().performClick()
        compose.waitUntil(8_000) { hasAny("12-week", "week", "One new article") }
        compose.onNodeWithContentDescription("Back").performClick()

        tapTab("Profile")
        compose.waitUntil(8_000) { hasAny("About", "Reminders") }
        compose.onNodeWithText("Medical Sources & Disclaimer").performScrollTo().performClick()
        compose.waitUntil(8_000) { hasAny("Medical sources") }
        compose.onNodeWithText("Medical sources").assertIsDisplayed()
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

    private fun becomeSignedIn() = runBlocking {
        session.signIn(email = "az-walk@example.com", name = "Az Walk", userId = "az-walk-user")
        withTimeout(10_000) { while (!session.awaitSignedIn()) delay(50) }
    }
}
