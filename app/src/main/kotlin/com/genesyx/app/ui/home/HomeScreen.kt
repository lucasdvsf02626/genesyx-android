package com.genesyx.app.ui.home

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.genesyx.app.ui.components.BrandOrb
import com.genesyx.app.ui.navigation.Screen
import com.genesyx.app.ui.theme.ElectricBlue
import com.genesyx.app.ui.theme.ElectricLavender

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(20.dp))

        // ── Greeting header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "Good afternoon",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
                Text(
                    text = state.userName,
                    style = MaterialTheme.typography.headlineMedium,
                    color = colors.onBackground,
                )
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(colors.surface),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = state.userName.firstOrNull()?.uppercase() ?: "G",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.onSurface,
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // ── Cycle hero card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            onClick = { /* TODO: open CycleSettings sheet */ },
        ) {
            Box(Modifier.fillMaxWidth()) {
                BrandOrb(
                    modifier = Modifier.align(Alignment.TopEnd),
                    size = 176.dp,
                )
                Column(Modifier.padding(24.dp)) {
                    Text(
                        text = state.cycleEyebrow,
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricLavender,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = state.cycleHeadline,
                        style = MaterialTheme.typography.headlineMedium,
                        color = colors.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = state.cycleSub,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // ── Today's focus card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    text = "TODAY'S FOCUS",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = state.todayFocus ?: "Complete your cycle setup to see focus foods.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.onSurface,
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // ── Stat grid (hydration + streak)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                icon = { Icon(Icons.Outlined.WaterDrop, null, tint = ElectricBlue) },
                label = "HYDRATION",
                value = state.hydrationLitres?.let { "%.1fL".format(it) } ?: "—",
                suffix = "/ ${"%.1f".format(state.hydrationGoalLitres)}L",
            )
            StatCard(
                modifier = Modifier.weight(1f),
                icon = { Icon(Icons.Outlined.Eco, null, tint = ElectricLavender) },
                label = "STREAK",
                value = state.streakDays?.toString() ?: "—",
                suffix = "days",
            )
        }

        Spacer(Modifier.height(20.dp))

        // ── Log today CTA
        Button(
            onClick = { navController.navigate(Screen.Log.route) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ElectricLavender),
        ) {
            Icon(Icons.Filled.Add, null, tint = Color.White)
            Spacer(Modifier.size(8.dp))
            Text(
                text = "Log today",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
            )
        }

        Spacer(Modifier.height(12.dp))

        // ── Pregnancy preview link
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .padding(horizontal = 4.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Preview pregnancy pathway",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
            Text(text = "→", color = colors.onSurfaceVariant, fontSize = 16.sp)
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
    label: String,
    value: String,
    suffix: String,
) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            icon()
            Spacer(Modifier.height(8.dp))
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(text = value, style = MaterialTheme.typography.titleLarge, color = colors.onSurface)
                Spacer(Modifier.size(4.dp))
                Text(
                    text = suffix,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 2.dp),
                )
            }
        }
    }
}
