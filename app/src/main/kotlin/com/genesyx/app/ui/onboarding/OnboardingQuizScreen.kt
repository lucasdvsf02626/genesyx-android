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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.genesyx.app.domain.content.DidYouKnow
import com.genesyx.app.domain.content.quizQuestions
import com.genesyx.app.ui.components.GxBackButton
import com.genesyx.app.ui.components.GxOptionPill
import com.genesyx.app.ui.components.GxPrimaryButton
import com.genesyx.app.ui.components.tintOnWhite
import com.genesyx.app.ui.theme.ElectricLavender
import com.genesyx.app.ui.theme.GenesyxTheme

@Composable
fun OnboardingQuizScreen(onComplete: () -> Unit, onBack: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val total = quizQuestions.size

    var step by remember { mutableStateOf(0) }
    val answers = remember { mutableStateMapOf<String, String>() }
    var pendingFact by remember { mutableStateOf<DidYouKnow?>(null) }

    val question = quizQuestions[step]
    val selected = answers[question.id]

    fun advance() {
        if (step == total - 1) onComplete() else step += 1
    }

    fun onContinue() {
        val fact = question.fact
        if (fact != null) pendingFact = fact else advance()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        // Header: back + progress + step counter
        Row(verticalAlignment = Alignment.CenterVertically) {
            GxBackButton(onClick = { if (step == 0) onBack() else step -= 1 })
            Spacer(Modifier.size(8.dp))
            LinearProgressIndicator(
                progress = { (step + 1).toFloat() / total },
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = ElectricLavender,
                trackColor = colors.surfaceVariant,
            )
            Spacer(Modifier.size(12.dp))
            Text(
                "${step + 1}/$total",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(32.dp))
        Text(
            question.question,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            color = colors.onBackground,
        )
        Spacer(Modifier.height(8.dp))
        Text(question.helper, style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)

        Spacer(Modifier.height(28.dp))
        question.options.forEach { option ->
            GxOptionPill(
                text = option.label,
                selected = selected == option.id,
                onClick = { answers[question.id] = option.id },
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }

        Spacer(Modifier.weight(1f))
        GxPrimaryButton(
            text = if (step == total - 1) "See My Summary" else "Continue",
            onClick = ::onContinue,
            enabled = selected != null,
        )
    }

    // "Did you know?" dialog
    val fact = pendingFact
    if (fact != null) {
        AlertDialog(
            onDismissRequest = { pendingFact = null },
            confirmButton = {
                GxPrimaryButton(
                    text = "Continue",
                    onClick = {
                        pendingFact = null
                        advance()
                    },
                )
            },
            icon = {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(ElectricLavender.tintOnWhite(0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.AutoAwesome, null, tint = ElectricLavender)
                }
            },
            title = {
                Text(fact.title, style = MaterialTheme.typography.titleLarge, color = colors.onSurface)
            },
            text = {
                Text(fact.body, style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
            },
            shape = RoundedCornerShape(28.dp),
            containerColor = colors.surface,
        )
    }
}

@Preview(name = "OnboardingQuiz — light", showBackground = true, showSystemUi = true)
@Composable
private fun OnboardingQuizScreenLightPreview() {
    GenesyxTheme(darkTheme = false) {
        OnboardingQuizScreen(onComplete = {}, onBack = {})
    }
}

@Preview(name = "OnboardingQuiz — dark", showBackground = true, showSystemUi = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun OnboardingQuizScreenDarkPreview() {
    GenesyxTheme(darkTheme = true) {
        OnboardingQuizScreen(onComplete = {}, onBack = {})
    }
}
