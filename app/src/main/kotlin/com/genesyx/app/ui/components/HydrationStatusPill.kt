package com.genesyx.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.genesyx.app.domain.hydration.HydrationPace
import com.genesyx.app.ui.theme.ElectricBlue
import com.genesyx.app.ui.theme.PhOptimal

/** The label for a hydration pace — one wording, shared by the Home card and the detail editor. */
fun hydrationStatusLabel(pace: HydrationPace): String = when (pace) {
    HydrationPace.REACHED -> "Goal reached"
    HydrationPace.AHEAD -> "Ahead of pace"
    HydrationPace.ON_TRACK -> "On track"
    HydrationPace.BEHIND -> "A little behind"
    HydrationPace.NOT_STARTED -> "Not started"
}

/** Colour for a pace — positive states carry brand colour; the rest stay neutral, never alarming. */
@Composable
fun hydrationStatusColor(pace: HydrationPace): Color = when (pace) {
    HydrationPace.REACHED, HydrationPace.AHEAD -> PhOptimal
    HydrationPace.ON_TRACK -> ElectricBlue
    HydrationPace.BEHIND, HydrationPace.NOT_STARTED -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
fun HydrationStatusPill(pace: HydrationPace, modifier: Modifier = Modifier) {
    val color = hydrationStatusColor(pace)
    Text(
        text = hydrationStatusLabel(pace).uppercase(),
        modifier = modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = 10.dp, vertical = 3.dp),
        fontSize = 10.5.sp,
        fontWeight = FontWeight.SemiBold,
        color = color,
    )
}
