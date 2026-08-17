package com.genesyx.app.domain.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QuizContentTest {

    @Test
    fun `gender is optional and unanswered is not prefer-not-to-say`() {
        val gender = quizQuestions.first { it.id == GENDER_QUESTION_ID }
        assertTrue(gender.optional)
        assertTrue(gender.canContinue(null))
        assertTrue(gender.canContinue(PREFER_NOT_TO_SAY))
        assertTrue(gender.options.any { it.id == PREFER_NOT_TO_SAY })
        assertFalse(gender.helper.lowercase().contains("guaranteed"))
        assertTrue(gender.helper.lowercase().contains("does not predict"))
        // Skipping leaves the key out of the saved map — that is unanswered, not prefer-not-to-say.
        val skipped = emptyMap<String, String>()
        assertNull(skipped[GENDER_QUESTION_ID])
        assertTrue(PREFER_NOT_TO_SAY !in skipped.values)
    }

    @Test
    fun `required questions still need a tap`() {
        val stage = quizQuestions.first { it.id == "stage" }
        assertFalse(stage.optional)
        assertFalse(stage.canContinue(null))
        assertTrue(stage.canContinue("trying"))
    }
}
