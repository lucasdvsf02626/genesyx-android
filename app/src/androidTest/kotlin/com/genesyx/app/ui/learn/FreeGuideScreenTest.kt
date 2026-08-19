package com.genesyx.app.ui.learn

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.genesyx.app.domain.content.FreeGuideContent
import com.genesyx.app.ui.theme.GenesyxTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FreeGuideScreenTest {

    @get:Rule val compose = createComposeRule()

    @Test
    fun first_and_last_pages_render() {
        compose.setContent {
            GenesyxTheme(darkTheme = false) {
                FreeGuideScreen(navController = rememberNavController())
            }
        }
        compose.onNodeWithText(FreeGuideContent.title).assertExists()
        compose.onNodeWithText("PAGE 1").assertExists()
        compose.onNodeWithText(FreeGuideContent.pages.first().heading).assertExists()
        compose.onNodeWithText("PAGE 20").assertExists()
        compose.onNodeWithText(FreeGuideContent.pages.last().heading).assertExists()
    }
}
