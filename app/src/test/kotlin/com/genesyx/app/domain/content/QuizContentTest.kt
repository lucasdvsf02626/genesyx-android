package com.genesyx.app.domain.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The onboarding quiz carries one compliance-sensitive question — the baby-sex preference. The
 * client's brief pins both its answers and its optionality, and an app that appears to act on a
 * stated sex preference is the single riskiest thing this product can ship. Both are pinned here so
 * neither can be loosened by accident.
 */
class QuizContentTest {

    private val genderQuestion = quizQuestions.first { it.id == "gender" }

    @Test
    fun `the baby-sex question offers exactly the four agreed answers, in order`() {
        assertEquals(
            listOf("girl", "boy", "no_preference", "prefer_not_to_say"),
            genderQuestion.options.map { it.id },
        )
        assertEquals(
            listOf("Girl", "Boy", "No preference", "Prefer not to say"),
            genderQuestion.options.map { it.label },
        )
    }

    @Test
    fun `the baby-sex question is optional`() {
        assertTrue(
            "The baby-sex preference must stay skippable — see the client brief, item 1C.",
            genderQuestion.optional,
        )
    }

    @Test
    fun `no other question is optional`() {
        quizQuestions.filter { it.id != "gender" }.forEach {
            assertFalse("Question '${it.id}' should be required", it.optional)
        }
    }

    @Test
    fun `no question promises an outcome it cannot deliver`() {
        // The app must never imply a baby's sex can be chosen or guaranteed. Guarding the quiz copy
        // the same way PhCopy and the Learn articles are guarded.
        val banned = listOf("guarantee", "guaranteed", "ensure", "choose the sex", "select the sex", "sway")
        val corpus = quizQuestions.flatMap {
            listOf(it.question, it.helper) + it.options.map { o -> o.label } +
                listOfNotNull(it.fact?.title, it.fact?.body)
        }
        corpus.forEach { text ->
            banned.forEach { phrase ->
                assertFalse(
                    "Quiz copy contains banned phrase \"$phrase\": \"$text\"",
                    text.lowercase().contains(phrase),
                )
            }
        }
    }
}
