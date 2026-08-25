package com.genesyx.app.data

import com.genesyx.app.core.log.Logger
import com.genesyx.app.core.result.DataResult
import com.genesyx.app.data.local.dao.ProfileDao
import com.genesyx.app.data.local.entity.ProfileEntity
import com.genesyx.app.data.remote.ProfileRemoteDataSource
import com.genesyx.app.data.remote.RemoteProfile
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileRepositoryThemeTest {

    private class FakeRemote : ProfileRemoteDataSource {
        val rows = mutableMapOf<String, RemoteProfile>()
        override suspend fun getProfile(userId: String) = DataResult.Success(rows[userId])
        override suspend fun upsertProfile(userId: String, profile: RemoteProfile): DataResult<Unit> {
            rows[userId] = profile
            return DataResult.Success(Unit)
        }
        override suspend fun updateDisplayName(userId: String, name: String) = DataResult.Success(Unit)
        override suspend fun updateTheme(userId: String, theme: String): DataResult<Unit> {
            rows[userId] = rows.getValue(userId).copy(theme = theme)
            return DataResult.Success(Unit)
        }
    }

    private class FakeDao : ProfileDao {
        val rows = mutableMapOf<String, ProfileEntity>()
        override fun observe(id: String): Flow<ProfileEntity?> = flowOf(rows[id])
        override suspend fun get(id: String): ProfileEntity? = rows[id]
        override suspend fun upsert(entity: ProfileEntity) { rows[entity.id] = entity }
    }

    private fun repo(dao: ProfileDao, remote: ProfileRemoteDataSource): ProfileRepository {
        val session = mockk<SessionRepository>(relaxed = true)
        every { session.userId } returns MutableStateFlow<String?>("u1")
        every { session.currentUserId() } returns "u1"
        coEvery { session.awaitUserId() } returns "u1"
        every { session.displayName } returns MutableStateFlow<String?>("Lucianne")
        return ProfileRepository(dao, remote, session, mockk<Logger>(relaxed = true))
    }

    @Test
    fun `a profile row this client creates is light, never dark`() = runTest {
        val remote = FakeRemote() // no row for u1 — refresh takes the createMissing path
        repo(FakeDao(), remote).refresh("u1")

        // The column default cannot save us here: createMissing spells the value out, so an
        // explicit "dark" would be a preference she never chose — and iOS honours this column
        // verbatim on sign-in, which is how it started booting dark.
        assertEquals("light", remote.rows.getValue("u1").theme)
    }

    @Test
    fun `an existing row's theme is left exactly as the server has it`() = runTest {
        val remote = FakeRemote().apply {
            rows["u1"] = RemoteProfile(displayName = "Lucianne", avatarUrl = null, partnerId = null, theme = "dark")
        }
        val dao = FakeDao()
        repo(dao, remote).refresh("u1")

        // A dark row that already exists may be a real choice. Correcting it is a server-side
        // decision, not something the client gets to make behind her back.
        assertEquals("dark", dao.rows.getValue("u1").theme)
        assertEquals("dark", remote.rows.getValue("u1").theme)
    }

    @Test
    fun `renaming an uncached profile does not invent a dark preference`() = runTest {
        val dao = FakeDao() // nothing cached — setDisplayName falls back to a fresh entity
        repo(dao, FakeRemote()).setDisplayName("Lucianne")

        assertEquals("light", dao.rows.getValue("u1").theme)
    }
}
