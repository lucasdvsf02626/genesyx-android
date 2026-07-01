package com.genesyx.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.genesyx.app.domain.model.CalendarCell
import com.genesyx.app.domain.model.DayType
import com.genesyx.app.ui.components.Eyebrow
import com.genesyx.app.ui.theme.BabyLavender
import com.genesyx.app.ui.theme.ElectricLavender
import com.genesyx.app.ui.theme.PowderBlue
import com.genesyx.app.ui.theme.PowderPink
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun TrackScreen(
    navController: NavController,
    viewModel: TrackViewModel = hiltViewModel(),
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
        Text("Track", style = MaterialTheme.typography.headlineMedium, color = colors.onBackground)
        Spacer(Modifier.height(20.dp))

        // Month navigator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = viewModel::prevMonth) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = colors.onBackground)
            }
            Text(
                "${state.month.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${state.month.year}",
                style = MaterialTheme.typography.titleLarge,
                color = colors.onBackground,
            )
            IconButton(onClick = viewModel::nextMonth) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = colors.onBackground)
            }
        }

        Spacer(Modifier.height(8.dp))

        // Day-of-week headers
        val dayHeaders = listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa")
        Row(Modifier.fillMaxWidth()) {
            dayHeaders.forEach { d ->
                Text(
                    d,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        if (state.cycleSetUp) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                userScrollEnabled = false,
            ) {
                items(state.cells) { cell ->
                    when (cell) {
                        is CalendarCell.Empty -> Box(Modifier.aspectRatio(1f))
                        is CalendarCell.Day -> {
                            val bg = when (cell.info.let { com.genesyx.app.domain.cycle.CycleEngine.dayTypeFor(it) }) {
                                DayType.PERIOD -> PowderPink.copy(alpha = 0.5f)
                                DayType.FERTILE -> PowderBlue.copy(alpha = 0.4f)
                                DayType.OVULATION -> ElectricLavender.copy(alpha = 0.7f)
                                DayType.LUTEAL -> BabyLavender.copy(alpha = 0.35f)
                                DayType.FOLLICULAR -> Color.Transparent
                            }
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .padding(2.dp)
                                    .clip(CircleShape)
                                    .background(bg)
                                    .then(
                                        if (cell.isToday) Modifier.border(1.5.dp, ElectricLavender, CircleShape)
                                        else Modifier
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    cell.date.dayOfMonth.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (cell.isToday) ElectricLavender else colors.onBackground,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Eyebrow("Legend")
            Spacer(Modifier.height(8.dp))
            val legend = listOf(
                "Period" to PowderPink,
                "Fertile" to PowderBlue,
                "Ovulation" to ElectricLavender,
                "Luteal" to BabyLavender,
            )
            legend.forEach { (label, color) ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                    Box(Modifier.size(12.dp).clip(CircleShape).background(color))
                    Text(" $label", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.surface),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Set up your cycle on the Home screen\nto see your calendar.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
