package com.genesyx.app.ui.onboarding

import androidx.lifecycle.ViewModel
import com.genesyx.app.data.QuizAnswersRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Persists the onboarding quiz answers. Completion happens before an account exists, so the answers
 * are written locally and pushed to the owner's `quiz_answers` row on sign-in (see
 * [QuizAnswersRepository]). Previously the answers were discarded on completion.
 */
@HiltViewModel
class OnboardingQuizViewModel @Inject constructor(
    private val quizAnswersRepository: QuizAnswersRepository,
) : ViewModel() {
    fun record(answers: Map<String, String>) = quizAnswersRepository.record(answers)
}
