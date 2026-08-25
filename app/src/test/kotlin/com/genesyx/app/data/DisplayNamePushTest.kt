package com.genesyx.app.data

import com.genesyx.app.core.log.Logger
import com.genesyx.app.core.result.DataResult
import com.genesyx.app.data.local.dao.ProfileDao
import com.genesyx.app.data.local.entity.ProfileEntity
import com.genesyx.app.data.remote.ProfileRemoteDataSource
import com.genesyx.app.data.remote.RemoteProfile
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * `profiles.display_name` holds the name she actually goes by; the session mirror the greeting reads
 * is seeded from her email address. These pin the three rules that decide which one wins, because
 * getting any of them wrong is worse than the mangled greeting being fixed:
 *
 * the address fallback is display-only, an empty column means no answer rather than blank her name,
 * and the push goes before the pull.
 */
class DisplayNamePushTest {

    private open class FakeRemote(var pushFails: Boolean = false) : ProfileRemoteDataSource {
        val rows = mutableMapOf<String, RemoteProfile>()

        /** Every call in order, so "push before pull" can be asserted as an ordering, not a count. */
        val calls = mutableListOf<String>()

        override suspend fun getProfile(userId: String): DataResult<RemoteProfile?> {
            calls += "get"
            return DataResult.Success(rows[userId])
        }

        override suspend fun upsertProfile(userId: String, profile: RemoteProfile): DataResult<Unit> {
            calls += "create"
            rows[userId] = profile
            return DataResult.Success(Unit)
        }

        override suspend fun updateDisplayName(userId: String, name: String): DataResult<Unit> {
            calls += "push"
            if (pushFails) return DataResult.Error(IllegalStateException("offline"), "offline")
            rows[userId] = rows[userId]?.copy(displayName = name)
                ?: RemoteProfile(name, null, null, "light")
            return DataResult.Success(Unit)
        }

        override suspend fun updateTheme(userId: String, theme: String) = DataResult.Success(Unit)
    }

    private class FakeDao : ProfileDao {
        val rows = mutableMapOf<String, ProfileEntity>()
        override fun observe(id: String): Flow<ProfileEntity?> = flowOf(rows[id])
        override suspend fun get(id: String): ProfileEntity? = rows[id]
        override suspend fun upsert(entity: ProfileEntity) { rows[entity.id] = entity }
    }

    private val onDevice = MutableStateFlow<String?>("Chezelle.madekwe")

    private fun session(nameOwed: Boolean) = mockk<SessionRepository>(relaxed = true).also {
        every { it.userId } returns MutableStateFlow<String?>("u1")
        every { it.currentUserId() } returns "u1"
        coEvery { it.awaitUserId() } returns "u1"
        every { it.displayName } returns onDevice
        coEvery { it.isNamePushOwed() } returns nameOwed
    }

    private fun repo(
        remote: ProfileRemoteDataSource,
        session: SessionRepository,
        dao: ProfileDao = FakeDao(),
    ) = ProfileRepository(dao, remote, session, mockk<Logger>(relaxed = true))

    private fun row(name: String?) =
        RemoteProfile(displayName = name, avatarUrl = null, partnerId = null, theme = "light")

    // ── The greeting ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `the name the server holds is the one that reaches the greeting`() = runTest {
        val remote = FakeRemote().apply { rows["u1"] = row("Chezelle Madekwe") }
        val session = session(nameOwed = false)

        repo(remote, session).refresh("u1")

        coVerify(exactly = 1) { session.adoptRemoteDisplayName("Chezelle Madekwe") }
    }

    /**
     * A row whose `display_name` was never filled in is silence, not an instruction. Reading it as
     * one would replace her name on Home with nothing at all — a regression on the bug being fixed.
     */
    @Test
    fun `an empty column is no answer, not an instruction to blank her name`() = runTest {
        val remote = FakeRemote().apply { rows["u1"] = row(null) }
        val session = session(nameOwed = false)

        repo(remote, session).refresh("u1")

        coVerify(exactly = 0) { session.adoptRemoteDisplayName(any()) }
    }

    @Test
    fun `a whitespace-only column is treated the same as an empty one`() = runTest {
        val remote = FakeRemote().apply { rows["u1"] = row("   ") }
        val session = session(nameOwed = false)

        repo(remote, session).refresh("u1")

        coVerify(exactly = 0) { session.adoptRemoteDisplayName(any()) }
    }

    // ── Push before pull ──────────────────────────────────────────────────────────────────────

    /**
     * The ordering IS the fix. Pull-then-push hands the stale server copy back first, so a rename
     * made offline is overwritten on screen before the push that would have saved it ever runs.
     */
    @Test
    fun `a name she typed is sent before the server copy is read`() = runTest {
        onDevice.value = "Chezelle Madekwe"
        val remote = FakeRemote().apply { rows["u1"] = row("Stale Name") }

        repo(remote, session(nameOwed = true)).refresh("u1")

        assertEquals(listOf("push", "get"), remote.calls)
        assertEquals("Chezelle Madekwe", remote.rows.getValue("u1").displayName)
    }

    @Test
    fun `a push that fails leaves the name still owed`() = runTest {
        onDevice.value = "Chezelle Madekwe"
        val remote = FakeRemote(pushFails = true).apply { rows["u1"] = row("Stale Name") }
        val session = session(nameOwed = true)

        repo(remote, session).refresh("u1")

        coVerify(exactly = 0) { session.clearNamePushOwed() }
    }

    /**
     * Clearing the flag says "the server has what this device holds". If she renamed again while
     * the push was in flight that is no longer true, and the newer name would never be sent.
     */
    @Test
    fun `a rename made during the push stays owed`() = runTest {
        onDevice.value = "First Name"
        val remote = object : FakeRemote() {
            override suspend fun updateDisplayName(userId: String, name: String): DataResult<Unit> {
                val result = super.updateDisplayName(userId, name)
                onDevice.value = "Second Name"
                return result
            }
        }
        val session = session(nameOwed = true)

        repo(remote, session).refresh("u1")

        coVerify(exactly = 0) { session.clearNamePushOwed() }
    }

    @Test
    fun `a push that lands stops the name being owed`() = runTest {
        onDevice.value = "Chezelle Madekwe"
        val session = session(nameOwed = true)

        repo(FakeRemote(), session).refresh("u1")

        coVerify(exactly = 1) { session.clearNamePushOwed() }
    }

    // ── The fallback stays display-only ───────────────────────────────────────────────────────

    /**
     * "Chezelle Madekwe" derived from `chezelle.madekwe@…` reads exactly like a real name, which is
     * what makes this dangerous: seeded into a fresh row it becomes the account's name everywhere,
     * and the real one is gone. Displaying a guess is a cosmetic bug; storing one is data loss.
     */
    @Test
    fun `a name derived from her address is never written into a new row`() = runTest {
        val remote = FakeRemote() // no row for u1 — refresh takes the create path

        repo(remote, session(nameOwed = false)).refresh("u1")

        assertNull(remote.rows.getValue("u1").displayName)
    }

    @Test
    fun `a name she gave us does seed a new row`() = runTest {
        onDevice.value = "Chezelle Madekwe"
        val remote = FakeRemote()

        repo(remote, session(nameOwed = true)).refresh("u1")

        assertEquals("Chezelle Madekwe", remote.rows.getValue("u1").displayName)
    }

    // ── The write-through path ────────────────────────────────────────────────────────────────

    @Test
    fun `a rename the server refuses stays owed for the next refresh to retry`() = runTest {
        onDevice.value = "Chezelle Madekwe"
        val session = session(nameOwed = true)

        repo(FakeRemote(pushFails = true), session).setDisplayName("Chezelle Madekwe")

        coVerify(exactly = 0) { session.clearNamePushOwed() }
    }
}
