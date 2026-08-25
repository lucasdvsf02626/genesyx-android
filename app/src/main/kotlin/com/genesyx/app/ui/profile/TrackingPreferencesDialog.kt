package com.genesyx.app.ui.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.genesyx.app.domain.content.quizQuestions
import com.genesyx.app.ui.components.ConsentWithdrawnBanner
import com.genesyx.app.ui.components.GxOptionPill
import com.genesyx.app.ui.components.SaveErrorText
import com.genesyx.app.ui.theme.ElectricLavender

/**
 * Edits the onboarding "Tracking Preferences" (the quiz answers), using the same stable question and
 * option ids as onboarding so the edit round-trips through the shared `quiz_answers` table. Answers
 * stay optional — a question with no selection simply isn't recorded, matching the onboarding flow
 * (which is why nothing here forces a choice).
 */
@Composable
fun TrackingPreferencesDialog(
    current: Map<String, String>,
    consentActive: Boolean = true,
    saving: Boolean = false,
    error: String? = null,
    onDismiss: () -> Unit,
    onSave: (Map<String, String>) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val answers = remember { mutableStateMapOf<String, String>().apply { putAll(current) } }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = colors.surface,
        title = { Text("Tracking Preferences") },
        text = {
            Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState())) {
                if (!consentActive) {
                    ConsentWithdrawnBanner()
                    Spacer(Modifier.height(16.dp))
                }
                quizQuestions.forEachIndexed { index, question ->
                    if (index > 0) Spacer(Modifier.height(16.dp))
                    Text(
                        question.question,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onSurface,
                    )
                    Spacer(Modifier.height(10.dp))
                    question.options.forEach { option ->
                        GxOptionPill(
                            text = option.label,
                            selected = answers[question.id] == option.id,
                            onClick = {
                                // Tapping the selected option again clears it — answers are optional.
                                if (answers[question.id] == option.id) {
                                    answers.remove(question.id)
                                } else {
                                    answers[question.id] = option.id
                                }
                            },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        )
                    }
                }

                SaveErrorText(error)
            }
        },
        confirmButton = {
            // The repository refuses the write while consent is withdrawn, so offering Save would
            // just dismiss the dialog and lose the edit silently.
            val canSave = consentActive && !saving
            TextButton(enabled = canSave, onClick = { onSave(answers.toMap()) }) {
                Text(
                    if (saving) "Saving…" else "Save",
                    color = if (canSave) ElectricLavender else colors.onSurfaceVariant.copy(alpha = 0.5f),
                    fontWeight = FontWeight.SemiBold,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = colors.onSurfaceVariant) }
        },
    )
}
