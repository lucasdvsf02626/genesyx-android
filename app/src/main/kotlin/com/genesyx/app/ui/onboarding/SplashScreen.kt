package com.genesyx.app.ui.onboarding

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.genesyx.app.R
import com.genesyx.app.ui.components.GxGhostButton
import com.genesyx.app.ui.components.GxPrimaryButton
import com.genesyx.app.ui.theme.ElectricLavender
import com.genesyx.app.ui.theme.GenesyxTheme

private data class Egg(
    val male: Boolean,
    val size: Int,
    val x: Int,
    val y: Int,
    val rotation: Float,
    val alpha: Float = 0.95f,
)

private val eggs = listOf(
    Egg(male = true, size = 150, x = -40, y = 40, rotation = 20f),
    Egg(male = false, size = 130, x = 300, y = 30, rotation = -25f),
    Egg(male = false, size = 96, x = -28, y = 320, rotation = 55f),
    Egg(male = true, size = 110, x = 300, y = 360, rotation = -15f),
    Egg(male = true, size = 80, x = 150, y = 180, rotation = 70f, alpha = 0.85f),
    Egg(male = false, size = 110, x = -36, y = 560, rotation = -50f),
    Egg(male = true, size = 140, x = 300, y = 600, rotation = 30f),
    Egg(male = false, size = 70, x = 240, y = 700, rotation = 0f, alpha = 0.85f),
)

@Composable
fun SplashScreen(onStart: () -> Unit, onSignIn: () -> Unit) {
    val colors = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        // Decorative floating eggs
        eggs.forEach { egg ->
            Image(
                painter = painterResource(if (egg.male) R.drawable.egg_male else R.drawable.egg_female),
                contentDescription = null,
                modifier = Modifier
                    .offset(x = egg.x.dp, y = egg.y.dp)
                    .size(egg.size.dp)
                    .rotate(egg.rotation)
                    .alpha(egg.alpha),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(8.dp))
            Text(
                "GENESYX",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = colors.onBackground,
                letterSpacing = 2.sp,
            )

            Spacer(Modifier.weight(1f))

            Text(
                "STEP INTO THE FUTURE OF FERTILITY",
                style = MaterialTheme.typography.labelSmall,
                color = ElectricLavender,
                letterSpacing = 2.4.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Feel informed, supported and ready for your conception journey.",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.SemiBold,
                color = colors.onBackground,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            Text(
                "A premium, gently-guided companion blending cycle awareness, nutrition and supplement support.",
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.weight(1f))

            GxPrimaryButton(
                text = "Start Your Personalised Quiz",
                onClick = onStart,
                trailingIcon = Icons.AutoMirrored.Filled.ArrowForward,
            )
            Spacer(Modifier.height(8.dp))
            GxGhostButton(text = "Sign in", onClick = onSignIn)
            Spacer(Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.AutoAwesome, null, tint = ElectricLavender, modifier = Modifier.size(14.dp))
                Spacer(Modifier.size(6.dp))
                Text(
                    "Educational fertility wellness support, tailored to you.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Preview(name = "Splash — light", showBackground = true, showSystemUi = true)
@Composable
private fun SplashScreenLightPreview() {
    GenesyxTheme(darkTheme = false) {
        SplashScreen(onStart = {}, onSignIn = {})
    }
}

@Preview(name = "Splash — dark", showBackground = true, showSystemUi = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SplashScreenDarkPreview() {
    GenesyxTheme(darkTheme = true) {
        SplashScreen(onStart = {}, onSignIn = {})
    }
}
