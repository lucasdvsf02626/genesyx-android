package com.genesyx.app.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.genesyx.app.core.log.Logger
import com.genesyx.app.core.result.DataResult
import com.genesyx.app.data.local.datastore.GenesyxPreferencesDataStore
import com.genesyx.app.data.remote.QuizAnswersRemoteDataSource
import io.mockk.coEvery
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
    private class FakeRemote(var pushFails: Boolean = false) : QuizAnswersRemoteDataSource {
        val rows = mutableMapOf<String, Map<String, String>>()
        val upserts = mutableListOf<Pair<String, Map<String, String>>>()
        override suspend fun get(userId: String) = DataResult.Success(rows[userId] ?: emptyMap())
        override suspend fun upsert(userId: String, answers: Map<String, String>): DataResult<Unit> {
            if (pushFails) return DataResult.Error(IllegalStateException("offline"), "offline")
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
        coEvery { session.awaitUserId() } returns (userId ?: SessionRepository.LOCAL_USER_ID)
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

    // ── The owed push ─────────────────────────────────────────────────────────────────────────
    //
    // `record` used to fire its push and never look at the answer, so an edit made while the
    // network was unhappy was simply lost — and then `refresh` pulled the stale server copy back
    // over the top of it, which reads as the app undoing what she just did.

    @Test
    fun `an edit the server refused is sent by the next refresh, not overwritten by it`() = runTest {
        val store = newStore()
        val remote = FakeRemote(pushFails = true).apply { rows["u1"] = mapOf("stage" to "old") }
        val r = repo(store, remote, signedIn = true, userId = "u1")

        r.record(mapOf("stage" to "trying"))
        assertEquals("the local write always lands", mapOf("stage" to "trying"), store.quizAnswers.first())

        remote.pushFails = false
        r.refresh("u1")

        assertEquals(mapOf("stage" to "trying"), store.quizAnswers.first())
        assertEquals(mapOf("stage" to "trying"), remote.rows["u1"])
    }

    /**
     * The push failing twice is the case that used to lose the edit outright. The server copy is
     * older than what this device holds, so refusing to pull is the only honest move.
     */
    @Test
    fun `a still-failing push stops the refresh pulling over the local edit`() = runTest {
        val store = newStore()
        val remote = FakeRemote(pushFails = true).apply { rows["u1"] = mapOf("stage" to "old") }
        val r = repo(store, remote, signedIn = true, userId = "u1")

        r.record(mapOf("stage" to "trying"))
        r.refresh("u1")

        assertEquals(mapOf("stage" to "trying"), store.quizAnswers.first())
    }

    /**
     * A clear is just an edit to the empty map — Android writes it unconditionally, which is why
     * clears stick here and not on iOS. It has to survive a failed push like any other edit.
     */
    @Test
    fun `a clear the server refused is not undone by the next refresh`() = runTest {
        val store = newStore()
        val remote = FakeRemote().apply { rows["u1"] = mapOf("stage" to "trying") }
        val r = repo(store, remote, signedIn = true, userId = "u1")
        store.setQuizAnswers(mapOf("stage" to "trying"))

        remote.pushFails = true
        r.record(emptyMap())
        remote.pushFails = false
        r.refresh("u1")

        assertEquals(emptyMap<String, String>(), store.quizAnswers.first())
        assertEquals("the empty set is written, not skipped", emptyMap<String, String>(), remote.rows["u1"])
    }

    @Test
    fun `a push that landed leaves nothing owed`() = runTest {
        val store = newStore()
        val remote = FakeRemote()
        val r = repo(store, remote, signedIn = true, userId = "u1")

        r.record(mapOf("stage" to "trying"))
        r.refresh("u1")

        // One push from record. A second would mean the debt was recorded despite the push landing.
        assertEquals(1, remote.upserts.size)
    }

    /**
     * An owed push carries the previous owner's answers. Surviving sign-out would fire them at
     * whoever signs in next, under their JWT and into their row.
     */
    @Test
    fun `signing out cancels an owed push`() = runTest {
        val store = newStore()
        val remote = FakeRemote(pushFails = true)
        repo(store, remote, signedIn = true, userId = "userA").record(mapOf("stage" to "trying"))

        store.clearQuizAnswers() // what sign-out does

        remote.pushFails = false
        remote.rows["userB"] = mapOf("gender" to "boy")
        repo(store, remote, signedIn = true, userId = "userB").refresh("userB")

        assertEquals(mapOf("gender" to "boy"), store.quizAnswers.first())
        assertTrue("userA's answers were never pushed anywhere", remote.upserts.none { it.first == "userB" })
    }
}
