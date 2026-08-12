package com.genesyx.app.data

import com.genesyx.app.core.di.ApplicationScope
import com.genesyx.app.core.log.Logger
import com.genesyx.app.core.result.DataResult
import com.genesyx.app.data.local.datastore.GenesyxPreferencesDataStore
import com.genesyx.app.data.remote.QuizAnswersRemoteDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Onboarding / tracking-preference answers, mirrored to the shared owner-only `quiz_answers` table
 * (cross-platform contract with iOS: a `jsonb` map of question-id → option-id).
 *
 * The lifecycle is the load-bearing part:
 * - Answered during onboarding *before* an account exists, so [record] writes locally and pushes
 *   only when signed in; a guest's answers wait in DataStore.
 * - [refresh] runs on sign-in: the server wins when it has answers (her real history), otherwise the
 *   local answers are adopted (the owed push a guest→account transition needs).
 * - [clearLocal] runs on sign-out. The local copy must go so it never lands on whoever signs in
 *   next; the server row is hers and stays, and comes back on her next sign-in. This is why there is
 *   no background queue — a queued write could fire under the wrong JWT.
 */
@Singleton
class QuizAnswersRepository @Inject constructor(
    private val store: GenesyxPreferencesDataStore,
    private val remote: QuizAnswersRemoteDataSource,
    private val session: SessionRepository,
    @ApplicationScope private val scope: CoroutineScope,
    private val logger: Logger,
) {
    /** The current answers — DataStore-backed, so they survive process death and drive the editor. */
    val answers: StateFlow<Map<String, String>> =
        store.quizAnswers.stateIn(scope, SharingStarted.Eagerly, emptyMap())

    /** Save answers locally, and push to the owner's row when signed in (captured user id). */
    fun record(answers: Map<String, String>) {
        scope.launch {
            store.setQuizAnswers(answers)
            val uid = signedInUserId() ?: return@launch
            remote.upsert(uid, answers)
        }
    }

    /** Sign-in reconciliation: server wins when it has answers; otherwise adopt the local copy. */
    suspend fun refresh(userId: String) {
        when (val r = remote.get(userId)) {
            is DataResult.Success -> {
                val server = r.data
                // Read the store directly (not the stateIn cache) so the decision reflects what is
                // actually persisted right now, even immediately after a write.
                val local = store.quizAnswers.first()
                if (server.isNotEmpty()) {
                    store.setQuizAnswers(server)
                } else if (local.isNotEmpty()) {
                    remote.upsert(userId, local) // owed push: guest answers → the account
                }
            }
            is DataResult.Error -> logger.w("QuizAnswers", "refresh skipped: ${r.message}")
            DataResult.Loading -> Unit
        }
    }

    /** Sign-out / delete: drop the local copy only; the server row is the owner's and is untouched. */
    fun clearLocal() {
        scope.launch { store.clearQuizAnswers() }
    }

    /** The real signed-in user id, or null for a guest (whose answers must not be pushed anywhere). */
    private fun signedInUserId(): String? =
        if (session.isSignedIn.value) session.currentUserId() else null
}
