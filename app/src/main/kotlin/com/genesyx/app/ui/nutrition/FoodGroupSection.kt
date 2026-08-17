package com.genesyx.app.ui.nutrition

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.genesyx.app.domain.content.FoodGroup
import com.genesyx.app.domain.content.FoodLogCopy
import com.genesyx.app.domain.content.nutritionPhaseFoodGroups
import com.genesyx.app.domain.model.Phase
import com.genesyx.app.ui.components.ExpandableInfo
import com.genesyx.app.ui.components.Eyebrow
import com.genesyx.app.ui.theme.ElectricLavender

/**
 * "What you ate today" — six chips. A record, not a scoreboard. A blank day costs her nothing.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FoodGroupCard(
    logged: Set<String>,
    phase: Phase?,
    onToggle: (String) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val known = FoodGroup.knownCount(logged)
    val total = FoodGroup.entries.size
    val emphasis = phase?.let { nutritionPhaseFoodGroups[it] }.orEmpty()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Eyebrow(FoodLogCopy.title, color = colors.onSurfaceVariant)
                Text(
                    "$known/$total",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                FoodLogCopy.summary(known, total),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurface,
            )
            Spacer(Modifier.height(12.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FoodGroup.entries.forEach { group ->
                    val selected = group.raw in logged
                    Text(
                        group.label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        color = if (selected) ElectricLavender else colors.onSurface,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (selected) ElectricLavender.copy(alpha = 0.12f) else colors.background,
                            )
                            .border(
                                1.dp,
                                if (selected) ElectricLavender else colors.outline,
                                RoundedCornerShape(20.dp),
                            )
                            .clickable { onToggle(group.raw) }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                    )
                }
            }
            FoodLogCopy.phaseLine(emphasis)?.let { line ->
                Spacer(Modifier.height(12.dp))
                Text(line, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
            Spacer(Modifier.height(8.dp))
            ExpandableInfo(
                label = FoodLogCopy.whatCountsLabel,
                body = FoodGroup.entries.joinToString("\n") { "${it.label}: ${it.examples}" },
            )
            Spacer(Modifier.height(8.dp))
            Text(
                FoodLogCopy.footnote,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        }
    }
}
