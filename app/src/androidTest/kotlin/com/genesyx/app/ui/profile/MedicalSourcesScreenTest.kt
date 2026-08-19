package com.genesyx.app.ui.profile

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.genesyx.app.domain.content.MEDICAL_DISCLAIMER
import com.genesyx.app.domain.content.MedicalSources
import com.genesyx.app.ui.theme.GenesyxTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MedicalSourcesScreenTest {

    @get:Rule val compose = createComposeRule()

    @Test
    fun catalogue_and_disclaimer_render() {
        compose.setContent {
            GenesyxTheme(darkTheme = false) {
                MedicalSourcesScreen(navController = rememberNavController())
            }
        }
        compose.onNodeWithText("Medical sources").assertExists()
        compose.onNodeWithText(MEDICAL_DISCLAIMER).assertExists()
        compose.onNodeWithContentDescription(
            "${MedicalSources.all.first().line}. Opens in your browser.",
        ).assertExists()
        compose.onNodeWithContentDescription(
            "${MedicalSources.all.last().line}. Opens in your browser.",
        ).assertExists()
    }
}
