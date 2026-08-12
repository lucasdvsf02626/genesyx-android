package com.genesyx.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The shared `quiz_answers` row: onboarding/tracking-preference answers, keyed by question id →
 * option id. Cross-platform contract with iOS (`answers` is a `jsonb` map of `[String: String]`).
 * `updated_at` is DB-defaulted and not sent from the client.
 */
@Serializable
data class QuizAnswersDto(
    @SerialName("user_id") val userId: String,
    val answers: Map<String, String> = emptyMap(),
)
