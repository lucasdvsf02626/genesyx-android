package com.genesyx.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.genesyx.app.ui.components.Eyebrow
import com.genesyx.app.ui.theme.ElectricLavender

@Composable
fun InsightsScreen(
    navController: NavController,
    viewModel: InsightsViewModel = hiltViewModel(),
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
        Text("Your Insights", style = MaterialTheme.typography.headlineMedium, color = colors.onBackground)
        Spacer(Modifier.height(20.dp))

        InsightCard(
            title = "Streak",
            value = if (state.streakDays != null) "${state.streakDays} days" else "—",
            sub = "Consecutive days logged",
        )
        Spacer(Modifier.height(12.dp))

        InsightCard(
            title = "Logs this month",
            value = state.logsThisMonth?.toString() ?: "—",
            sub = "Days you've tracked",
        )
        Spacer(Modifier.height(12.dp))

        if (state.topSymptoms.isNotEmpty()) {
            Eyebrow("Most frequent symptoms (last 30 days)")
            Spacer(Modifier.height(8.dp))
            state.topSymptoms.forEachIndexed { i, s ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    elevation = CardDefaults.cardElevation(0.dp),
                ) {
                    Row(Modifier.padding(16.dp)) {
                        Text("${i + 1}. ", color = ElectricLavender, style = MaterialTheme.typography.bodyLarge)
                        Text(s, style = MaterialTheme.typography.bodyLarge, color = colors.onSurface)
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                elevation = CardDefaults.cardElevation(0.dp),
            ) {
                Text(
                    "Log a few days to see symptom trends here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(20.dp),
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun InsightCard(title: String, value: String, sub: String) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(title.uppercase(), style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.headlineMedium, color = ElectricLavender)
            Text(sub, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        }
    }
}
