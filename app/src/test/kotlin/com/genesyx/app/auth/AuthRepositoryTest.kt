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
    private val database = mockk<GenesyxDatabase>(relaxed = true)
    private val logger = mockk<Logger>(relaxed = true)

    private fun repo(scope: CoroutineScope): AuthRepository {
        val d: CoroutineDispatcher = UnconfinedTestDispatcher()
        val dispatchers = object : DispatcherProvider {
            override val main = d
            override val io = d
            override val default = d
        }
        return AuthRepository(
            authService, session, profileRepo, cycleRepo, dailyLogRepo, phRepo, database,
            dispatchers, scope, logger,
        )
    }

    @Test
    fun `deleteAccount wipes local DB only AFTER server delete succeeds`() = runTest {
        coEvery { authService.deleteAccount() } returns DataResult.Success(Unit)

        val result = repo(backgroundScope).deleteAccount()

        assertTrue(result is DataResult.Success)
        coVerifyOrder {                              // server delete BEFORE wipe BEFORE sign-out
            authService.deleteAccount()
            database.clearAllTables()
            session.signOut()
        }
    }

    @Test
    fun `deleteAccount does NOT wipe local DB when server delete fails`() = runTest {
        coEvery { authService.deleteAccount() } returns DataResult.Error(RuntimeException("boom"))

        val result = repo(backgroundScope).deleteAccount()

        assertTrue(result is DataResult.Error)
        verify(exactly = 0) { database.clearAllTables() }   // data-safety invariant
        verify(exactly = 0) { session.signOut() }
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
        verify { session.signIn("a@b.co", "a", userId = "uid-123") }
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
        verify(exactly = 0) { session.signIn(any(), any(), any()) }
    }

    @Test
    fun `signInWithGoogle persists the session on success`() = runTest {
        val user = AuthUser(id = "g-uid", email = "g@b.co", displayName = "g", emailVerified = true)
        coEvery { authService.signInWithIdToken("tok-123") } returns
            DataResult.Success(AuthSession(user, accessToken = "at"))
        val appScope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))

        val result = repo(appScope).signInWithGoogle("tok-123")

        assertTrue(result is DataResult.Success)
        verify { session.signIn("g@b.co", "g", userId = "g-uid") }
        appScope.cancel()
    }

    @Test
    fun `signInWithGoogle surfaces error and does not persist a session`() = runTest {
        coEvery { authService.signInWithIdToken(any()) } returns
            DataResult.Error(RuntimeException("bad token"))

        val result = repo(backgroundScope).signInWithGoogle("bad")

        assertTrue(result is DataResult.Error)
        verify(exactly = 0) { session.signIn(any(), any(), any()) }
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
            session.signOut()
        }
    }

    @Test
    fun `signOut clears local session and data even when the remote sign-out fails`() = runTest {
        // Offline: the remote call fails, but "Log out" must still log the user out locally —
        // otherwise the button silently does nothing and they stay signed in.
        coEvery { authService.signOut() } returns DataResult.Error(RuntimeException("offline"))

        val result = repo(backgroundScope).signOut()

        assertTrue(result is DataResult.Success)
        coVerify { database.clearAllTables() }
        verify { session.signOut() }
    }
}
