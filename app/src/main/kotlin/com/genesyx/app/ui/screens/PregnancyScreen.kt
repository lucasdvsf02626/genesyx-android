package com.genesyx.app.ui.screens

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
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.genesyx.app.data.SessionRepository
import com.genesyx.app.ui.components.Eyebrow
import com.genesyx.app.ui.components.GxBackButton
import com.genesyx.app.ui.components.GxGhostButton
import com.genesyx.app.ui.components.GxPrimaryButton
import com.genesyx.app.ui.components.tintOnWhite
import com.genesyx.app.ui.theme.ElectricPink
import com.genesyx.app.ui.theme.PowderPink
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class PregnancyViewModel @Inject constructor(
    sessionRepository: SessionRepository,
) : ViewModel() {
    val displayName: StateFlow<String?> = sessionRepository.displayName
}

@Composable
fun PregnancyScreen(onBack: () -> Unit, viewModel: PregnancyViewModel = hiltViewModel()) {
    var switched by remember { mutableStateOf(false) }
    val displayName by viewModel.displayName.collectAsState()

    if (switched) {
        PregnancyHome(displayName = displayName ?: "Guest", onBackToPrep = { switched = false })
    } else {
        PregnancyTransition(onSwitch = { switched = true }, onLater = onBack)
    }
}

@Composable
private fun PregnancyTransition(onSwitch: () -> Unit, onLater: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        GxBackButton(onClick = onLater)

        Spacer(Modifier.height(8.dp))
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.size(80.dp).clip(RoundedCornerShape(24.dp)).background(PowderPink.tintOnWhite(0.30f)),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.Favorite, null, tint = ElectricPink, modifier = Modifier.size(36.dp)) }
            Spacer(Modifier.height(24.dp))
            Text(
                "Support for the next chapter",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = colors.onBackground,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Whenever you're ready, Genesyx can gently shift to support you through pregnancy — at your pace.",
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(Modifier.height(28.dp))
        FeatureCard(Icons.Filled.ChildCare, "Trimester tracking", "Week-by-week guidance with calm, clear updates.")
        Spacer(Modifier.height(12.dp))
        FeatureCard(Icons.Filled.Restaurant, "Prenatal nutrition", "Updated focus foods and supplement guidance.")

        Spacer(Modifier.height(32.dp))
        GxPrimaryButton(text = "Switch to pregnancy mode", onClick = onSwitch)
        Spacer(Modifier.height(4.dp))
        GxGhostButton(text = "Not yet, keep tracking", onClick = onLater)
    }
}

@Composable
private fun FeatureCard(icon: ImageVector, title: String, desc: String) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(PowderPink.tintOnWhite(0.25f)),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, null, tint = ElectricPink) }
            Spacer(Modifier.size(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
                Spacer(Modifier.height(2.dp))
                Text(desc, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun PregnancyHome(displayName: String, onBackToPrep: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(20.dp))
        Text("Pregnancy mode", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        Text(displayName, style = MaterialTheme.typography.headlineMedium, color = colors.onBackground)

        Spacer(Modifier.height(24.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column(Modifier.padding(24.dp)) {
                Eyebrow("Week-by-week", color = ElectricPink)
                Spacer(Modifier.height(10.dp))
                Text("Gentle prenatal guidance", style = MaterialTheme.typography.headlineMedium.copy(fontSize = 24.sp), color = colors.onSurface)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Once you confirm your due date, Genesyx will guide you through each week with calm prenatal nutrition, symptom tracking, and supplement reminders.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(Icons.Filled.ChildCare, "Trimester", "—", ElectricPink, Modifier.weight(1f))
        }

        Spacer(Modifier.height(20.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column(Modifier.padding(20.dp)) {
                Text("Prenatal essentials", style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
                Spacer(Modifier.height(8.dp))
                listOf(
                    "Folate 400–800 mcg daily",
                    "Vitamin D 600 IU daily",
                    "Omega-3 (DHA) 200 mg daily",
                    "Stay hydrated and rest when needed",
                ).forEach {
                    Text("• $it", style = MaterialTheme.typography.bodyLarge, color = colors.onSurface.copy(alpha = 0.85f), modifier = Modifier.padding(vertical = 3.dp))
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        OutlinedButton(
            onClick = onBackToPrep,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(16.dp),
        ) { Text("Switch back to fertility prep", color = colors.onSurface) }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun StatTile(icon: ImageVector, label: String, value: String, tint: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(16.dp))
            Spacer(Modifier.height(10.dp))
            Eyebrow(label, color = colors.onSurfaceVariant)
            Spacer(Modifier.height(2.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, color = colors.onSurface)
        }
    }
}
