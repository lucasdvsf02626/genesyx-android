package com.genesyx.app.ui.onboarding

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.genesyx.app.ui.components.GxBackButton
import com.genesyx.app.ui.components.GxPrimaryButton
import com.genesyx.app.ui.components.tintOnWhite
import com.genesyx.app.ui.theme.ElectricBlue
import com.genesyx.app.ui.theme.ElectricLavender
import com.genesyx.app.ui.theme.ElectricPink
import com.genesyx.app.ui.theme.GenesyxTheme
import com.genesyx.app.ui.theme.PowderBlue
import com.genesyx.app.ui.theme.PowderPink

private data class Benefit(
    val icon: ImageVector,
    val iconTint: Color,
    val iconBg: Color,
    val title: String,
    val desc: String,
)

@Composable
fun OnboardingIntroScreen(onContinue: () -> Unit, onBack: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val benefits = listOf(
        Benefit(Icons.Outlined.FavoriteBorder, ElectricLavender, ElectricLavender.tintOnWhite(0.12f),
            "Understand your cycle", "Recognise patterns with calm, clear guidance."),
        Benefit(Icons.Outlined.Eco, ElectricBlue, PowderBlue.tintOnWhite(0.30f),
            "Support fertility nutrition", "Cycle-aware food and supplement focus."),
        Benefit(Icons.Outlined.BarChart, ElectricPink, PowderPink.tintOnWhite(0.30f),
            "Receive tailored insights", "Gentle observations based on your tracking."),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        GxBackButton(onClick = onBack, modifier = Modifier.padding(start = 0.dp))

        Spacer(Modifier.height(8.dp))
        Text(
            "Your fertility preparation, gently guided",
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.SemiBold,
            color = colors.onBackground,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "Genesyx blends cycle awareness, nutrition, and supportive insights into one calm space.",
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onSurfaceVariant,
        )

        Spacer(Modifier.height(32.dp))
        benefits.forEach { b ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(b.iconBg),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(b.icon, null, tint = b.iconTint)
                    }
                    Spacer(Modifier.size(16.dp))
                    Column {
                        Text(b.title, style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
                        Spacer(Modifier.height(2.dp))
                        Text(b.desc, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        GxPrimaryButton(text = "Continue", onClick = onContinue)
    }
}

@Preview(name = "OnboardingIntro — light", showBackground = true, showSystemUi = true)
@Composable
private fun OnboardingIntroScreenLightPreview() {
    GenesyxTheme(darkTheme = false) {
        OnboardingIntroScreen(onContinue = {}, onBack = {})
    }
}

@Preview(name = "OnboardingIntro — dark", showBackground = true, showSystemUi = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun OnboardingIntroScreenDarkPreview() {
    GenesyxTheme(darkTheme = true) {
        OnboardingIntroScreen(onContinue = {}, onBack = {})
    }
}
