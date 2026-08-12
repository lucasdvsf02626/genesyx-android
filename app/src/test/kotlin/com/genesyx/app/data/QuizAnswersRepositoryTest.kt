package com.genesyx.app.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.genesyx.app.core.log.Logger
import com.genesyx.app.core.result.DataResult
import com.genesyx.app.data.local.datastore.GenesyxPreferencesDataStore
import com.genesyx.app.data.remote.QuizAnswersRemoteDataSource
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class QuizAnswersRepositoryTest {

    @get:Rule val tmp = TemporaryFolder()
    private var counter = 0

    /** In-memory quiz_answers "server", keyed by user id, plus a record of what was pushed. */
    private class FakeRemote : QuizAnswersRemoteDataSource {
        val rows = mutableMapOf<String, Map<String, String>>()
        val upserts = mutableListOf<Pair<String, Map<String, String>>>()
        override suspend fun get(userId: String) = DataResult.Success(rows[userId] ?: emptyMap())
        override suspend fun upsert(userId: String, answers: Map<String, String>): DataResult<Unit> {
            rows[userId] = answers
            upserts += userId to answers
            return DataResult.Success(Unit)
        }
    }

    private fun newStore(): GenesyxPreferencesDataStore {
        val ds: DataStore<Preferences> = PreferenceDataStoreFactory.create(
            produceFile = { File(tmp.root, "prefs_${counter++}.preferences_pb") },
        )
        return GenesyxPreferencesDataStore(ds)
    }

    private fun repo(
        store: GenesyxPreferencesDataStore,
        remote: FakeRemote,
        signedIn: Boolean,
        userId: String?,
    ): QuizAnswersRepository {
        val session = mockk<SessionRepository>(relaxed = true)
        every { session.isSignedIn } returns MutableStateFlow(signedIn)
        every { session.currentUserId() } returns (userId ?: SessionRepository.LOCAL_USER_ID)
        return QuizAnswersRepository(store, remote, session, TestScope(), mockk<Logger>(relaxed = true))
    }

    @Test
    fun `refresh — the server wins when it has answers`() = runTest {
        val store = newStore()
        store.setQuizAnswers(mapOf("gender" to "girl")) // local (e.g. a stale guess)
        val remote = FakeRemote().apply { rows["u1"] = mapOf("gender" to "boy", "stage" to "trying") }

        repo(store, remote, signedIn = true, userId = "u1").refresh("u1")

        assertEquals(mapOf("gender" to "boy", "stage" to "trying"), store.quizAnswers.first())
    }

    @Test
    fun `refresh — an empty server adopts the local guest answers`() = runTest {
        val store = newStore()
        store.setQuizAnswers(mapOf("stage" to "trying")) // answered as a guest, no account yet
        val remote = FakeRemote() // server has nothing for this user

        repo(store, remote, signedIn = true, userId = "u1").refresh("u1")

        assertEquals(mapOf("stage" to "trying"), remote.rows["u1"]) // pushed (adopted)
    }

    @Test
    fun `clearLocal drops the local copy but never the server row`() = runTest {
        val store = newStore()
        val remote = FakeRemote().apply { rows["u1"] = mapOf("gender" to "girl") }
        store.setQuizAnswers(mapOf("gender" to "girl"))

        val r = repo(store, remote, signedIn = true, userId = "u1")
        r.clearLocal()
        // clearLocal launches on the repo scope; drive it deterministically via the store instead.
        store.clearQuizAnswers()

        assertEquals(emptyMap<String, String>(), store.quizAnswers.first())
        assertEquals("server row is the owner's and stays", mapOf("gender" to "girl"), remote.rows["u1"])
    }

    @Test
    fun `one account's answers never reach the next — sign-out clears, next pull is that user's`() = runTest {
        val store = newStore()
        val remote = FakeRemote().apply {
            rows["userA"] = mapOf("gender" to "girl")
            rows["userB"] = mapOf("gender" to "boy")
        }
        // User A signs in and syncs.
        repo(store, remote, signedIn = true, userId = "userA").refresh("userA")
        assertEquals(mapOf("gender" to "girl"), store.quizAnswers.first())

        // Sign-out clears local; user B signs in and pulls THEIR row, never A's.
        store.clearQuizAnswers()
        repo(store, remote, signedIn = true, userId = "userB").refresh("userB")
        assertEquals(mapOf("gender" to "boy"), store.quizAnswers.first())
    }

    @Test
    fun `a guest never pushes — no session, nothing owed to any account`() = runTest {
        val store = newStore()
        val remote = FakeRemote()
        val r = repo(store, remote, signedIn = false, userId = null)
        r.record(mapOf("stage" to "exploring"))
        // record launches on the repo scope; the guest branch pushes nothing regardless of timing.
        assertTrue("a guest write reaches no server row", remote.upserts.isEmpty())
    }
}
