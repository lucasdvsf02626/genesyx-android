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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.genesyx.app.ui.components.BrandLockup
import com.genesyx.app.ui.components.Eyebrow
import com.genesyx.app.ui.components.GxGhostButton
import com.genesyx.app.ui.components.GxPrimaryButton
import com.genesyx.app.ui.components.tintOnWhite
import com.genesyx.app.ui.theme.ElectricLavender
import com.genesyx.app.ui.theme.GenesyxTheme
import com.genesyx.app.ui.theme.PowderBlue

private data class Insight(val icon: ImageVector, val label: String, val value: String)

@Composable
fun ReadinessSummaryScreen(onUnlockGuide: () -> Unit, onContinue: () -> Unit, onBack: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val insights = listOf(
        Insight(Icons.Outlined.CalendarMonth, "Cycle awareness", "Build a steady tracking rhythm"),
        Insight(Icons.Outlined.Eco, "Nutrition focus", "Folate, omega-3, and zinc-rich foods"),
        Insight(Icons.Filled.AutoAwesome, "Daily support", "Gentle prompts and supplement plan"),
    )
    val nextSteps = listOf(
        "Start logging your cycle for 7 days",
        "Review your personalised nutrition focus",
        "Save the free fertility nutrition guide",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(24.dp))
        BrandLockup(height = 32.dp)
        Spacer(Modifier.height(20.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(ElectricLavender.tintOnWhite(0.10f))
                .padding(horizontal = 14.dp, vertical = 6.dp),
        ) {
            Eyebrow("Your readiness summary", color = ElectricLavender)
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "A thoughtful starting point",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            color = colors.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "You're already taking meaningful steps. Here's where Genesyx will support you next.",
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(28.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column(Modifier.padding(20.dp)) {
                insights.forEachIndexed { i, it ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(ElectricLavender.tintOnWhite(0.10f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(it.icon, null, tint = ElectricLavender)
                        }
                        Spacer(Modifier.size(14.dp))
                        Column {
                            Eyebrow(it.label, color = colors.onSurfaceVariant)
                            Spacer(Modifier.height(2.dp))
                            Text(it.value, style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
                        }
                    }
                    if (i < insights.lastIndex) Spacer(Modifier.height(16.dp))
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = PowderBlue.tintOnWhite(0.18f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column(Modifier.padding(20.dp)) {
                Text("Suggested next steps", style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
                Spacer(Modifier.height(12.dp))
                nextSteps.forEach { step ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                        Icon(Icons.Filled.Check, null, tint = ElectricLavender, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(10.dp))
                        Text(step, style = MaterialTheme.typography.bodyLarge, color = colors.onSurface)
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        GxPrimaryButton(
            text = "Unlock My Free Guide",
            onClick = onUnlockGuide,
            leadingIcon = Icons.AutoMirrored.Outlined.MenuBook,
        )
        Spacer(Modifier.height(4.dp))
        GxGhostButton(text = "Register / Login to continue", onClick = onContinue)
    }
}

@Preview(name = "ReadinessSummary — light", showBackground = true, showSystemUi = true)
@Composable
private fun ReadinessSummaryScreenLightPreview() {
    GenesyxTheme(darkTheme = false) {
        ReadinessSummaryScreen(onUnlockGuide = {}, onContinue = {}, onBack = {})
    }
}

@Preview(name = "ReadinessSummary — dark", showBackground = true, showSystemUi = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ReadinessSummaryScreenDarkPreview() {
    GenesyxTheme(darkTheme = true) {
        ReadinessSummaryScreen(onUnlockGuide = {}, onContinue = {}, onBack = {})
    }
}
