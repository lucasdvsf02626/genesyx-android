package com.genesyx.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.outlined.ChildCare
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LocalDining
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.genesyx.app.ui.components.BrandOrb
import com.genesyx.app.ui.components.GxBackButton
import com.genesyx.app.ui.components.GxGhostButton
import com.genesyx.app.ui.components.GxPrimaryButton
import com.genesyx.app.ui.theme.ElectricLavender
import com.genesyx.app.ui.theme.ElectricPink
import kotlinx.coroutines.launch

private data class Feature(val icon: ImageVector, val title: String, val desc: String)
private val features = listOf(
    Feature(Icons.Outlined.LocalDining, "Prenatal nutrition", "Trimester-specific food and supplement guidance."),
    Feature(Icons.Outlined.ChildCare, "Trimester tracking", "Week-by-week development insights."),
    Feature(Icons.Outlined.FavoriteBorder, "Symptom support", "Understand and ease common pregnancy symptoms."),
    Feature(Icons.Outlined.Spa, "Mindful movement", "Safe exercise and relaxation guidance."),
)

@Composable
fun PregnancyScreen(onBack: () -> Unit) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme

    fun comingSoon() { scope.launch { snackbar.showSnackbar("Coming in a future update ✨") } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        GxBackButton(onClick = onBack)

        Spacer(Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = ElectricPink.copy(alpha = 0.08f)),
            elevation = CardDefaults.cardElevation(0.dp),
        ) {
            Box(Modifier.fillMaxWidth()) {
                BrandOrb(modifier = Modifier.align(Alignment.TopEnd), size = 160.dp)
                Column(Modifier.padding(24.dp)) {
                    Text("PREVIEW", style = MaterialTheme.typography.labelSmall, color = ElectricPink)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Support for the next chapter",
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onBackground,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Genesyx Pregnancy mode is in development. Here's a preview of what's coming.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        features.forEach { f ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                elevation = CardDefaults.cardElevation(0.dp),
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(ElectricLavender.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(f.icon, null, tint = ElectricLavender)
                    }
                    Column(Modifier.padding(start = 16.dp)) {
                        Text(f.title, style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
                        Text(f.desc, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        GxPrimaryButton(text = "Join the waitlist", onClick = ::comingSoon)
        Spacer(Modifier.height(8.dp))
        GxGhostButton(text = "Not right now", onClick = onBack)

        SnackbarHost(snackbar)
        Spacer(Modifier.height(24.dp))
    }
}
