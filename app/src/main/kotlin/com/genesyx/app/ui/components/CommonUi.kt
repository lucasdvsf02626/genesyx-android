package com.genesyx.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.genesyx.app.R
import com.genesyx.app.ui.theme.ElectricLavender

/** Tints a brand color over a white card (approximates the web `color-mix(color X%, white)`). */
fun Color.tintOnWhite(fraction: Float): Color = copy(alpha = fraction)

/** ALL-CAPS section eyebrow (labelSmall). */
@Composable
fun Eyebrow(text: String, color: Color = MaterialTheme.colorScheme.onSurfaceVariant, modifier: Modifier = Modifier) {
    Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, color = color, modifier = modifier)
}

/**
 * Genesyx primary lockup (G mark + wordmark). The dark-ink wordmark is unreadable on a dark
 * background, so the asset is picked from the active color scheme rather than a `drawable-night`
 * qualifier — the in-app theme toggle never changes the system `uiMode` the qualifier reads.
 * [height] controls the wordmark height; width scales to keep the aspect ratio.
 */
@Composable
fun BrandLockup(modifier: Modifier = Modifier, height: Dp = 30.dp) {
    val onDarkBackground = MaterialTheme.colorScheme.background.luminance() < 0.5f
    Image(
        painter = painterResource(
            if (onDarkBackground) R.drawable.brand_lockup_dark else R.drawable.brand_lockup,
        ),
        contentDescription = "Genesyx",
        modifier = modifier.height(height),
        contentScale = ContentScale.Fit,
    )
}

/** Back chevron button (48dp hit area — Android minimum touch target). */
@Composable
fun GxBackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onClick, modifier = modifier.size(48.dp)) {
        Icon(
            Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = MaterialTheme.colorScheme.onBackground,
        )
    }
}

/** Tall pill primary CTA (h-56, rounded-28, electric-lavender). */
@Composable
fun GxPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = ElectricLavender,
            contentColor = Color.White,
            disabledContainerColor = ElectricLavender.copy(alpha = 0.45f),
            disabledContentColor = Color.White,
        ),
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, null)
            Spacer(Modifier.size(8.dp))
        }
        Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        if (trailingIcon != null) {
            Spacer(Modifier.size(8.dp))
            Icon(trailingIcon, null)
        }
    }
}

/** Low-emphasis text button. */
@Composable
fun GxGhostButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(onClick = onClick, modifier = modifier) {
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
        )
    }
}

/** Quiz / selectable option row with a trailing radio that fills + checks when selected. */
@Composable
fun GxOptionPill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) ElectricLavender.tintOnWhite(0.10f) else colors.surface)
            .border(
                BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) ElectricLavender else colors.outline),
                RoundedCornerShape(20.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = if (selected) ElectricLavender else colors.onSurface,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(if (selected) ElectricLavender else Color.Transparent)
                .border(
                    BorderStroke(if (selected) 0.dp else 1.5.dp, colors.outline),
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
    }
}
