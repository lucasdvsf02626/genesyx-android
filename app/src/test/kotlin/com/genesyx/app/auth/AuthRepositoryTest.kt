package com.genesyx.app.auth

import com.genesyx.app.core.DispatcherProvider
import com.genesyx.app.core.log.Logger
import com.genesyx.app.core.result.DataResult
import com.genesyx.app.data.CycleRepository
import com.genesyx.app.data.DailyLogRepository
import com.genesyx.app.data.PhRepository
import com.genesyx.app.data.ProfileRepository
import com.genesyx.app.data.SessionRepository
import com.genesyx.app.data.local.GenesyxDatabase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthRepositoryTest {

    private val authService = mockk<AuthService>()
    private val session = mockk<SessionRepository>(relaxed = true)
    private val profileRepo = mockk<ProfileRepository>(relaxed = true)
    private val cycleRepo = mockk<CycleRepository>(relaxed = true)
    private val dailyLogRepo = mockk<DailyLogRepository>(relaxed = true)
    private val phRepo = mockk<PhRepository>(relaxed = true)
    private val userSupplementRepo = mockk<com.genesyx.app.data.UserSupplementRepository>(relaxed = true)
    private val consentRepo = mockk<com.genesyx.app.data.ConsentRepository>(relaxed = true)
    private val quizAnswersRepo = mockk<com.genesyx.app.data.QuizAnswersRepository>(relaxed = true)
    private val supplementReminderRepo = mockk<com.genesyx.app.data.SupplementReminderRepository>(relaxed = true)
    private val syncStatusRepo = mockk<com.genesyx.app.data.SyncStatusRepository>(relaxed = true)
    private val preferences = mockk<com.genesyx.app.data.local.datastore.GenesyxPreferencesDataStore>(relaxed = true)
    private val database = mockk<GenesyxDatabase>(relaxed = true)
    private val reminderScheduler = mockk<com.genesyx.app.notifications.ReminderScheduler>(relaxed = true)
    private val logger = mockk<Logger>(relaxed = true)

    private fun repo(scope: CoroutineScope): AuthRepository {
        val d: CoroutineDispatcher = UnconfinedTestDispatcher()
        val dispatchers = object : DispatcherProvider {
            override val main = d
            override val io = d
            override val default = d
        }
        return AuthRepository(
            authService, session, consentRepo, profileRepo, cycleRepo, dailyLogRepo, phRepo, userSupplementRepo,
            quizAnswersRepo, supplementReminderRepo, syncStatusRepo, preferences, database, reminderScheduler,
            dispatchers, scope, logger,
        )
    }

    @Test
    fun `deleteAccount wipes local DB only AFTER server delete succeeds`() = runTest {
        coEvery { authService.deleteAccount() } returns DataResult.Success(Unit)

        val result = repo(backgroundScope).deleteAccount()

        assertTrue(result is DataResult.Success)
        coVerifyOrder {                              // server delete BEFORE wipe BEFORE prefs clear
            authService.deleteAccount()
            database.clearAllTables()
            preferences.clearAll()
        }
    }

    @Test
    fun `deleteAccount clears supplement reminders too`() = runTest {
        // They live in DataStore on their own scheduler, so clearAllTables and reminderScheduler
        // both miss them. Left behind, they keep firing notifications naming her supplements long
        // after the account is gone. Awaited (clearAllNow), not launched — the UI reports success
        // only after the cancels have run.
        coEvery { authService.deleteAccount() } returns DataResult.Success(Unit)

        repo(backgroundScope).deleteAccount()

        coVerify { supplementReminderRepo.clearAllNow() }
    }

    @Test
    fun `deleteAccount cancels the sync drains and clears the whole prefs file`() = runTest {
        // Left enqueued, the drains wake against a dead session; left populated, focus mode, cycle
        // phase and notification pacing are inherited by whoever uses the device next.
        coEvery { authService.deleteAccount() } returns DataResult.Success(Unit)

        repo(backgroundScope).deleteAccount()

        verify { syncStatusRepo.cancelDrains() }
        coVerify { preferences.clearAll() }
    }

    @Test
    fun `deleteAccount does NOT wipe local DB when server delete fails`() = runTest {
        coEvery { authService.deleteAccount() } returns DataResult.Error(RuntimeException("boom"))

        val result = repo(backgroundScope).deleteAccount()

        assertTrue(result is DataResult.Error)
        verify(exactly = 0) { database.clearAllTables() }   // data-safety invariant
        coVerify(exactly = 0) { preferences.clearAll() }
    }

    @Test
    fun `a deletion retry that finds the account already gone finally runs the wipe`() = runTest {
        // The dropped-response scenario: the server committed the delete but the reply was lost, so
        // the first attempt surfaces as Error and nothing local may be touched. On retry the
        // service recognises "no authenticated user" and reports Success — and the wipe, which was
        // unreachable on this path before, must now run to completion.
        coEvery { authService.deleteAccount() } returns DataResult.Error(RuntimeException("timeout"))
        val repository = repo(backgroundScope)

        assertTrue(repository.deleteAccount() is DataResult.Error)
        verify(exactly = 0) { database.clearAllTables() }

        coEvery { authService.deleteAccount() } returns DataResult.Success(Unit)

        assertTrue(repository.deleteAccount() is DataResult.Success)
        coVerifyOrder {
            database.clearAllTables()
            preferences.clearAll()
        }
        verify { syncStatusRepo.cancelDrains() }
    }

    @Test
    fun `changeEmail delegates but does NOT touch the persisted session`() = runTest {
        // Success only means "confirmation email sent" — the address flips server-side after the
        // link is followed, so persisting the new email here would show an address that isn't real.
        coEvery { authService.changeEmail("pw", "new@b.co") } returns DataResult.Success(Unit)

        val result = repo(backgroundScope).changeEmail("pw", "new@b.co")

        assertTrue(result is DataResult.Success)
        coVerify { authService.changeEmail("pw", "new@b.co") }
        coVerify(exactly = 0) { session.signInNow(any(), any(), any()) }
    }

    @Test
    fun `signInWithPassword persists the session with the auth uid`() = runTest {
        val user = AuthUser(id = "uid-123", email = "a@b.co", displayName = "a", emailVerified = true)
        coEvery { authService.signInWithPassword(any(), any()) } returns
            DataResult.Success(AuthSession(user, accessToken = "tok"))
        // Unconfined appScope so the fire-and-forget background refresh launch runs eagerly.
        val appScope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))

        val result = repo(appScope).signInWithPassword("a@b.co", "pw")

        assertTrue(result is DataResult.Success)
        coVerify { session.signInNow("a@b.co", "a", userId = "uid-123") }
        coVerify { profileRepo.refresh("uid-123") }         // background read-through fired
        appScope.cancel()
    }

    @Test
    fun `signInWithPassword surfaces error and does not persist a session`() = runTest {
        // Regression: a failed sign-in (e.g. wrong password / stale-session masquerade rejected
        // by SupabaseAuthService) must NOT leave the user in any session. Previously a failed
        // sign-in could land the user back in a prior still-valid session.
        coEvery { authService.signInWithPassword(any(), any()) } returns
            DataResult.Error(RuntimeException("Invalid login credentials"))

        val result = repo(backgroundScope).signInWithPassword("nope@b.co", "wrong")

        assertTrue(result is DataResult.Error)
        coVerify(exactly = 0) { session.signInNow(any(), any(), any()) }
    }

    @Test
    fun `signUp persists the name she typed, not the one the provider invented`() = runTest {
        // SupabaseAuthService builds its AuthUser from the address alone, so the displayName it
        // returns is the localpart. Trusting it filed her as "lucianne.valenca" and Home greeted
        // her by that — her own name, spelled wrong, on the first line of the app.
        val user = AuthUser(
            id = "u1",
            email = "lucianne.valenca@example.com",
            displayName = "lucianne.valenca",
            emailVerified = true,
        )
        coEvery { authService.signUp(any(), any(), any()) } returns
            DataResult.Success(AuthSession(user, accessToken = "tok"))
        val appScope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))

        repo(appScope).signUp("lucianne.valenca@example.com", "pw", "Lucianne Valença")

        coVerify { session.signInNow("lucianne.valenca@example.com", "Lucianne Valença", userId = "u1") }
        appScope.cancel()
    }

    @Test
    fun `signInWithGoogle persists the session on success`() = runTest {
        val user = AuthUser(id = "g-uid", email = "g@b.co", displayName = "g", emailVerified = true)
        coEvery { authService.signInWithIdToken("tok-123") } returns
            DataResult.Success(AuthSession(user, accessToken = "at"))
        val appScope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))

        val result = repo(appScope).signInWithGoogle("tok-123")

        assertTrue(result is DataResult.Success)
        coVerify { session.signInNow("g@b.co", "g", userId = "g-uid") }
        appScope.cancel()
    }

    @Test
    fun `signInWithGoogle surfaces error and does not persist a session`() = runTest {
        coEvery { authService.signInWithIdToken(any()) } returns
            DataResult.Error(RuntimeException("bad token"))

        val result = repo(backgroundScope).signInWithGoogle("bad")

        assertTrue(result is DataResult.Error)
        coVerify(exactly = 0) { session.signInNow(any(), any(), any()) }
    }

    @Test
    fun `signOut ends the remote session, wipes local data, then clears the session`() = runTest {
        // Sign-out used to clear the DataStore key and nothing else: the provider session survived
        // (so a later sign-up could inherit it) and every Room row stayed on disk, readable by the
        // next person to use the device.
        coEvery { authService.signOut() } returns DataResult.Success(Unit)

        val result = repo(backgroundScope).signOut()

        assertTrue(result is DataResult.Success)
        coVerifyOrder {
            authService.signOut()
            database.clearAllTables()
            preferences.clearAll()
        }
    }

    @Test
    fun `sendPasswordReset addresses the typed email and does not persist a session`() = runTest {
        // She is at the signed-out gate. The reset must go to what she typed — there is no
        // session email to fall back on — and asking for the email must never grant a session.
        coEvery { authService.resetPassword("ada@example.com") } returns DataResult.Success(Unit)

        val result = repo(backgroundScope).sendPasswordReset("ada@example.com")

        assertTrue(result is DataResult.Success)
        coVerify { authService.resetPassword("ada@example.com") }
        coVerify(exactly = 0) { session.signInNow(any(), any(), any()) }
    }

    @Test
    fun `sendPasswordReset surfaces a failed send rather than reporting success`() = runTest {
        // Swallowing a failure would leave her looking at a confirmation for an email that was
        // never sent — and she has no other route in, so she would simply wait.
        coEvery { authService.resetPassword(any()) } returns
            DataResult.Error(RuntimeException("offline"))

        val result = repo(backgroundScope).sendPasswordReset("ada@example.com")

        assertTrue(result is DataResult.Error)
        coVerify(exactly = 0) { session.signInNow(any(), any(), any()) }
    }

    // ── Code-23: granting consent re-runs the sign-in sync that was consent-refused. ──────────

    @Test
    fun `granting consent re-runs the skipped health-data sync exactly once`() = runTest {
        coEvery { session.awaitUserId() } returns "uid-123"
        coEvery { consentRepo.isCollectionPermitted() } returns true
        val appScope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))

        repo(appScope).resyncAfterConsentGranted()

        coVerify(exactly = 1) { dailyLogRepo.refresh("uid-123") }
        coVerify(exactly = 1) { phRepo.refresh("uid-123") }
        coVerify(exactly = 1) { userSupplementRepo.refresh("uid-123") }
        coVerify(exactly = 1) { cycleRepo.refresh("uid-123") }
        coVerify(exactly = 1) { quizAnswersRepo.refresh("uid-123") }
        coVerify(exactly = 1) { profileRepo.refresh("uid-123") }
        appScope.cancel()
    }

    @Test
    fun `no resync when collection is still not permitted — a failed grant must trigger nothing`() = runTest {
        coEvery { session.awaitUserId() } returns "uid-123"
        coEvery { consentRepo.isCollectionPermitted() } returns false
        val appScope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))

        repo(appScope).resyncAfterConsentGranted()

        coVerify(exactly = 0) { dailyLogRepo.refresh(any()) }
        coVerify(exactly = 0) { phRepo.refresh(any()) }
        appScope.cancel()
    }

    @Test
    fun `no resync for a guest — there is no server data to pull`() = runTest {
        coEvery { session.awaitUserId() } returns com.genesyx.app.data.SessionRepository.LOCAL_USER_ID
        val appScope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))

        repo(appScope).resyncAfterConsentGranted()

        coVerify(exactly = 0) { dailyLogRepo.refresh(any()) }
        coVerify(exactly = 0) { consentRepo.isCollectionPermitted() }
        appScope.cancel()
    }

    @Test
    fun `signOut clears local session and data even when the remote sign-out fails`() = runTest {
        // Offline: the remote call fails, but "Log out" must still log the user out locally —
        // otherwise the button silently does nothing and they stay signed in.
        coEvery { authService.signOut() } returns DataResult.Error(RuntimeException("offline"))

        val result = repo(backgroundScope).signOut()

        assertTrue(result is DataResult.Success)
        coVerify { database.clearAllTables() }
        coVerify { preferences.clearAll() }
    }
}
