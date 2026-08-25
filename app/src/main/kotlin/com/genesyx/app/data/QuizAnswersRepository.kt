package com.genesyx.app.data

import com.genesyx.app.core.di.ApplicationScope
import com.genesyx.app.core.log.Logger
import com.genesyx.app.core.result.DataResult
import com.genesyx.app.core.result.SaveOutcome
import com.genesyx.app.data.local.datastore.GenesyxPreferencesDataStore
import com.genesyx.app.data.remote.QuizAnswersRemoteDataSource
import com.genesyx.app.domain.consent.HealthDataCollectionGate
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
    // Hilt always supplies the real gate (BindingsModule binds ConsentRepository). The permissive
    // default exists so a test can construct this without standing up Room, mirroring iOS.
    private val consent: HealthDataCollectionGate = HealthDataCollectionGate { true },
) {
    /** The current answers — DataStore-backed, so they survive process death and drive the editor. */
    val answers: StateFlow<Map<String, String>> =
        store.quizAnswers.stateIn(scope, SharingStarted.Eagerly, emptyMap())

    /**
     * Save answers locally, and push to the owner's row when signed in (captured user id).
     *
     * Suspends and reports rather than launching into [scope] — the editor keeps its dialog open
     * until it knows, so a refusal or a failed push can't look like a save that took.
     *
     * A guest has no row to push to, so a local write is the whole job and reports [SaveOutcome.Saved].
     */
    suspend fun record(answers: Map<String, String>): SaveOutcome {
        if (!consent.isCollectionPermitted()) {
            logger.w("QuizAnswers", "record refused — health-data consent withdrawn")
            return SaveOutcome.Refused
        }
        store.setQuizAnswers(answers)
        val uid = signedInUserId() ?: return SaveOutcome.Saved
        return when (val result = remote.upsert(uid, answers)) {
            is DataResult.Error -> {
                logger.w("QuizAnswers", "remote upsert failed: ${result.message}")
                // Record the debt. Without it the push is simply lost, and the next [refresh]
                // pulls the stale server copy back over the top of her edit — the same visible
                // symptom as the iOS clear bug, but for any edit made while the network is unhappy.
                store.setQuizAnswersOwed(true)
                SaveOutcome.Failed(result.message)
            }
            else -> {
                // Re-read first: an edit made while this push was in flight is still owed.
                if (store.quizAnswers.first() == answers) store.setQuizAnswersOwed(false)
                SaveOutcome.Saved
            }
        }
    }

    /**
     * Sign-in reconciliation: push what's owed, then let the server win when it has answers;
     * otherwise adopt the local copy.
     *
     * The order is the fix. Pulling first hands back a server copy that is older than an edit this
     * device failed to push, so the edit is silently undone — and a full clear looks like the
     * answers came back by themselves. Refused outright after a withdrawal: this is the pull that
     * would otherwise reinstate the tracking answers she just stopped consenting to.
     */
    suspend fun refresh(userId: String) {
        if (!consent.isCollectionPermitted()) {
            logger.w("QuizAnswers", "refresh refused — health-data consent withdrawn")
            return
        }
        // Read the store directly (not the stateIn cache) so the decision reflects what is actually
        // persisted right now, even immediately after a write.
        val local = store.quizAnswers.first()
        val owed = store.quizAnswersOwed.first()
        if (owed && !drain(userId, local)) return

        when (val r = remote.get(userId)) {
            is DataResult.Success -> {
                val server = r.data
                if (server.isNotEmpty()) {
                    store.setQuizAnswers(server)
                } else if (local.isNotEmpty() && !owed) {
                    remote.upsert(userId, local) // owed push: guest answers → the account
                }
            }
            is DataResult.Error -> logger.w("QuizAnswers", "refresh skipped: ${r.message}")
            DataResult.Loading -> Unit
        }
    }

    /**
     * Send an edit the server never took. Returns false when it still hasn't landed, which stops
     * the caller pulling: the local copy is the newer of the two, and reading the server's would
     * discard the very edit this is trying to save.
     */
    private suspend fun drain(userId: String, local: Map<String, String>): Boolean {
        if (remote.upsert(userId, local) is DataResult.Error) {
            logger.w("QuizAnswers", "owed answers push deferred — not pulling over the local copy")
            return false
        }
        if (store.quizAnswers.first() == local) store.setQuizAnswersOwed(false)
        return true
    }

    /** Sign-out / delete: drop the local copy only; the server row is the owner's and is untouched. */
    fun clearLocal() {
        scope.launch { store.clearQuizAnswers() }
    }

    /** The real signed-in user id, or null for a guest (whose answers must not be pushed anywhere). */
    private fun signedInUserId(): String? =
        if (session.isSignedIn.value) session.currentUserId() else null
}
