package com.genesyx.app.data.remote

import com.genesyx.app.core.log.Logger
import com.genesyx.app.core.result.DataResult
import com.genesyx.app.data.remote.dto.QuizAnswersDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real Supabase (`quiz_answers` table) implementation. Owner-only under RLS (`user_id = auth.uid()`),
 * so the signed-in session's JWT scopes both the read and the write. Bound only when creds are
 * configured (see NetworkModule); otherwise the no-op stub is used.
 */
@Singleton
class SupabaseQuizAnswersRemoteDataSource @Inject constructor(
    private val client: SupabaseClient,
    private val logger: Logger,
) : QuizAnswersRemoteDataSource {

    override suspend fun get(userId: String): DataResult<Map<String, String>> =
        try {
            val dto = client.from("quiz_answers")
                .select { filter { eq("user_id", userId) } }
                .decodeSingleOrNull<QuizAnswersDto>()
            DataResult.Success(dto?.answers ?: emptyMap())
        } catch (t: Throwable) {
            logger.e("QuizAnswers", "get failed", t)
            DataResult.Error(t, t.message)
        }

    override suspend fun upsert(userId: String, answers: Map<String, String>): DataResult<Unit> =
        try {
            client.from("quiz_answers").upsert(QuizAnswersDto(userId = userId, answers = answers))
            DataResult.Success(Unit)
        } catch (t: Throwable) {
            logger.e("QuizAnswers", "upsert failed", t)
            DataResult.Error(t, t.message)
        }
}
