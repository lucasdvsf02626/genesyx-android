package com.genesyx.app.ui.onboarding

import android.content.res.Configuration
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.genesyx.app.ui.components.BrandOrb
import com.genesyx.app.ui.components.Eyebrow
import com.genesyx.app.ui.components.GxGhostButton
import com.genesyx.app.ui.components.GxPrimaryButton
import com.genesyx.app.ui.theme.ElectricLavender
import com.genesyx.app.ui.theme.GenesyxTheme

private val EMAIL_REGEX = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

@Composable
fun WaitlistScreen(onContinue: () -> Unit, onBack: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    var email by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(24.dp))

        if (submitted) {
            Spacer(Modifier.height(40.dp))
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(ElectricLavender),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Check, null, tint = colors.onPrimary, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.height(20.dp))
            Text(
                "You're on the list",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = colors.onBackground,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "We'll send your free fertility nutrition guide to $email shortly.",
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(28.dp))
            GxPrimaryButton(text = "Register / Login to continue", onClick = onContinue)
            return@Column
        }

        // eBook hero
        Box(
            modifier = Modifier
                .size(width = 176.dp, height = 224.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(ElectricLavender.copy(alpha = 0.10f)),
        ) {
            Column(Modifier.padding(20.dp)) {
                Eyebrow("Genesyx", color = ElectricLavender)
                Spacer(Modifier.height(12.dp))
                Text(
                    "The Fertility Nutrition Guide",
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.onSurface,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "A gentle companion for eating with intention.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
                Spacer(Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Eyebrow("Edition 01", color = colors.onSurfaceVariant)
                    Spacer(Modifier.weight(1f))
                    BrandOrb(size = 28.dp)
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Eyebrow("Free with early access", color = ElectricLavender)
        Spacer(Modifier.height(8.dp))
        Text(
            "A gentle guide to fertility nutrition",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            color = colors.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Sent straight to your inbox when you join the Genesyx waiting list.",
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(20.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it; error = null },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("your@email.com") },
            singleLine = true,
            isError = error != null,
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        )
        if (error != null) {
            Spacer(Modifier.height(6.dp))
            Text(error!!, style = MaterialTheme.typography.bodyMedium, color = colors.error, modifier = Modifier.fillMaxWidth())
        }

        Spacer(Modifier.height(16.dp))
        GxPrimaryButton(
            text = "Join the Waiting List",
            onClick = {
                if (EMAIL_REGEX.matches(email.trim())) submitted = true
                else error = "Please enter a valid email address."
            },
        )
        Spacer(Modifier.height(4.dp))
        GxGhostButton(text = "Register / Login to continue", onClick = onContinue)
    }
}

@Preview(name = "Waitlist — light", showBackground = true, showSystemUi = true)
@Composable
private fun WaitlistScreenLightPreview() {
    GenesyxTheme(darkTheme = false) {
        WaitlistScreen(onContinue = {}, onBack = {})
    }
}

@Preview(name = "Waitlist — dark", showBackground = true, showSystemUi = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WaitlistScreenDarkPreview() {
    GenesyxTheme(darkTheme = true) {
        WaitlistScreen(onContinue = {}, onBack = {})
    }
}
