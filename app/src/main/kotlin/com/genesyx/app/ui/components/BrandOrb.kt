package com.genesyx.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.genesyx.app.ui.theme.BabyLavender
import com.genesyx.app.ui.theme.PowderPink
import com.genesyx.app.ui.theme.PrimaryLight

/**
 * Decorative "BrandOrb" — the soft pearl/gradient blob used across splash, results,
 * the Home hero card, and pregnancy. Approximates the web `.gx-orb` radial gradient.
 */
@Composable
fun BrandOrb(
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color.White,
                        PrimaryLight.copy(alpha = 0.45f),
                        BabyLavender.copy(alpha = 0.35f),
                        PowderPink.copy(alpha = 0.30f),
                    ),
                ),
            ),
    )
}
