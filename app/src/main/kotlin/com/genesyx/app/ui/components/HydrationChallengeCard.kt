package com.genesyx.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.genesyx.app.ui.home.HYDRATION_CHALLENGE_TARGET
import com.genesyx.app.ui.theme.ElectricBlue

/**
 * The 7-day "stay hydrated" challenge: log water seven days running. Progress is the hydration
 * streak (capped at 7); a filled row of dots and encouraging, non-guilt copy — the same tone as the
 * streak. Rolls forward on its own as the streak grows; celebrates on completion. Shared by Home
 * and the Nutrition tab, so both show the same number.
 */
@Composable
fun HydrationChallengeCard(days: Int, onOpen: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val target = HYDRATION_CHALLENGE_TARGET
    val done = days >= target
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(ElectricBlue.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Outlined.WaterDrop, null, tint = ElectricBlue, modifier = Modifier.size(20.dp)) }
                Spacer(Modifier.size(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (done) "Hydration challenge complete!" else "7-day hydration challenge",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.onSurface,
                    )
                    Text(
                        when {
                            done -> "Seven days running — beautifully hydrated. Keep it going."
                            days == 0 -> "Log water today to start your 7-day streak."
                            else -> "$days of $target days — a glass a day keeps it going."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(target) { i ->
                    Box(
                        Modifier
                            .weight(1f)
                            .height(8.dp)
                            .clip(CircleShape)
                            .background(if (i < days) ElectricBlue else colors.surfaceVariant.copy(alpha = 0.6f)),
                    )
                }
            }
        }
    }
}
