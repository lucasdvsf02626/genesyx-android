package com.genesyx.app.data

import com.genesyx.app.core.log.Logger
import com.genesyx.app.core.result.DataResult
import com.genesyx.app.data.local.dao.ProfileDao
import com.genesyx.app.data.local.entity.ProfileEntity
import com.genesyx.app.data.remote.ProfileRemoteDataSource
import com.genesyx.app.data.remote.RemoteProfile
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local-first profile store. Room ([ProfileDao]) is the source of truth the UI observes; the
 * Supabase-backed [ProfileRemoteDataSource] provides read-through (on sign-in) and write-through
 * (on change). Scoped per signed-in user. Keeps working offline / unconfigured via the stub remote.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class ProfileRepository @Inject constructor(
    private val dao: ProfileDao,
    private val remote: ProfileRemoteDataSource,
    private val session: SessionRepository,
    private val logger: Logger,
) {
    /** The signed-in user's cached profile (Room). */
    val profile: Flow<ProfileEntity?> =
        session.userId.flatMapLatest { uid -> dao.observe(uid ?: SessionRepository.LOCAL_USER_ID) }

    /**
     * Pull the remote profile row into the local cache. If the row is missing (e.g. a user created
     * before the signup trigger existed), create it. Returns the cached entity.
     */
    suspend fun refresh(userId: String = session.currentUserId()): DataResult<ProfileEntity?> =
        when (val result = remote.getProfile(userId)) {
            is DataResult.Success -> {
                val entity = result.data?.toEntity(userId) ?: createMissing(userId)
                dao.upsert(entity)
                logger.i("Profile", "cached profile for $userId (theme=${entity.theme})")
                DataResult.Success(entity)
            }
            is DataResult.Error -> {
                logger.e("Profile", "refresh failed", result.throwable)
                result
            }
            DataResult.Loading -> DataResult.Loading
        }

    /** Write-through: update the display name locally and remotely. */
    suspend fun setDisplayName(name: String): DataResult<Unit> {
        val userId = session.currentUserId()
        val current = dao.get(userId) ?: ProfileEntity(userId, null, null, null, "dark")
        dao.upsert(current.copy(displayName = name))
        return remote.updateDisplayName(userId, name)
    }

    /** Write-through: update the theme locally and remotely. */
    suspend fun setTheme(theme: String): DataResult<Unit> {
        val userId = session.currentUserId()
        val current = dao.get(userId) ?: ProfileEntity(userId, null, null, null, "dark")
        dao.upsert(current.copy(theme = theme))
        return remote.updateTheme(userId, theme)
    }

    private suspend fun createMissing(userId: String): ProfileEntity {
        val fallback = RemoteProfile(
            displayName = session.displayName.value,
            avatarUrl = null,
            partnerId = null,
            theme = "dark",
        )
        remote.upsertProfile(userId, fallback)
        return fallback.toEntity(userId)
    }

    private fun RemoteProfile.toEntity(userId: String) = ProfileEntity(
        id = userId,
        displayName = displayName,
        avatarUrl = avatarUrl,
        partnerId = partnerId,
        theme = theme,
    )
}
