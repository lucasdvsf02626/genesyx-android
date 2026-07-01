package com.genesyx.app.ui.screens

import androidx.compose.foundation.background
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
import com.genesyx.app.domain.content.FocusFood
import com.genesyx.app.domain.content.phaseFoods
import com.genesyx.app.domain.content.phaseHeroCopy
import com.genesyx.app.domain.content.phaseLabel
import com.genesyx.app.ui.components.Eyebrow
import com.genesyx.app.ui.theme.ElectricLavender

@Composable
fun NutritionScreen(
    navController: NavController,
    viewModel: NutritionViewModel = hiltViewModel(),
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
        Text("Nutrition", style = MaterialTheme.typography.headlineMedium, color = colors.onBackground)
        Spacer(Modifier.height(4.dp))

        val phase = state.phase
        if (phase != null) {
            val hero = phaseHeroCopy[phase]
            val label = phaseLabel[phase] ?: ""
            Text(label, style = MaterialTheme.typography.labelSmall, color = ElectricLavender)
            Spacer(Modifier.height(4.dp))
            Text(hero?.hero ?: "", style = MaterialTheme.typography.titleLarge, color = colors.onBackground)
            Spacer(Modifier.height(4.dp))
            Text(hero?.sub ?: "", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)

            Spacer(Modifier.height(24.dp))
            Eyebrow("Focus foods")
            Spacer(Modifier.height(12.dp))

            phaseFoods[phase]?.forEach { food ->
                FoodCard(food)
                Spacer(Modifier.height(10.dp))
            }

            Spacer(Modifier.height(8.dp))
            Eyebrow("Today's focus")
            Spacer(Modifier.height(8.dp))
            hero?.focus?.let { f ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = ElectricLavender.copy(alpha = 0.10f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(f.title, style = MaterialTheme.typography.titleMedium, color = ElectricLavender)
                        Spacer(Modifier.height(4.dp))
                        Text(f.body, style = MaterialTheme.typography.bodyMedium, color = colors.onSurface)
                    }
                }
            }
        } else {
            Spacer(Modifier.height(20.dp))
            Text(
                "Set up your cycle on the Home screen to see nutrition guidance.",
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun FoodCard(food: FocusFood) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(food.title, style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
            Spacer(Modifier.height(2.dp))
            Text(food.desc, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        }
    }
}
