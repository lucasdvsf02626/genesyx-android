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
    suspend fun refresh(userId: String = session.currentUserId()): DataResult<ProfileEntity?> {
        // Read before draining clears it: it is the only record of whether the name on this device
        // is one she gave us or one we derived from her address, and createMissing needs to know.
        val nameWasGiven = session.isNamePushOwed()
        // Push before pull. A rename made offline is owed to the server, and pulling first would
        // hand back the stale copy and quietly undo it.
        drainPendingName(userId)
        return when (val result = remote.getProfile(userId)) {
            is DataResult.Success -> {
                val entity = result.data?.toEntity(userId) ?: createMissing(userId, nameWasGiven)
                dao.upsert(entity)
                // The greeting reads the session mirror, which is seeded from the auth session — an
                // address-derived guess. This is the only thing that lets the real name in.
                // A blank column means the row has no answer, not that her name should be blanked.
                entity.displayName?.takeIf { it.isNotBlank() }
                    ?.let { session.adoptRemoteDisplayName(it) }
                logger.i("Profile", "cached profile (theme=${entity.theme})")
                DataResult.Success(entity)
            }
            is DataResult.Error -> {
                logger.e("Profile", "refresh failed", result.throwable)
                result
            }
            DataResult.Loading -> DataResult.Loading
        }
    }

    /**
     * Send a name she gave us that the server has not confirmed holding, and stop owing it only
     * once the push succeeds — a failed push stays owed rather than being dropped on the floor.
     *
     * The name is re-read before the flag is cleared: a rename made while this push was in flight
     * is a different name, and clearing on it would strand the newer one.
     */
    private suspend fun drainPendingName(userId: String) {
        if (!session.isNamePushOwed()) return
        val name = session.displayName.value?.takeIf { it.isNotBlank() } ?: return
        if (remote.updateDisplayName(userId, name) is DataResult.Error) {
            logger.w("Profile", "display-name push deferred — still owed")
            return
        }
        if (session.displayName.value == name) session.clearNamePushOwed()
    }

    /**
     * Write-through: update the display name locally and remotely.
     *
     * On failure the name stays owed, so the next [refresh] pushes it rather than pulling the
     * stale server copy over the top of it.
     */
    suspend fun setDisplayName(name: String): DataResult<Unit> {
        val userId = session.currentUserId()
        val current = dao.get(userId) ?: ProfileEntity(userId, null, null, null, "light")
        dao.upsert(current.copy(displayName = name))
        val result = remote.updateDisplayName(userId, name)
        if (result !is DataResult.Error && session.displayName.value == name) {
            session.clearNamePushOwed()
        }
        return result
    }

    /** Write-through: update the theme locally and remotely. */
    suspend fun setTheme(theme: String): DataResult<Unit> {
        val userId = session.currentUserId()
        val current = dao.get(userId) ?: ProfileEntity(userId, null, null, null, "light")
        dao.upsert(current.copy(theme = theme))
        return remote.updateTheme(userId, theme)
    }

    private suspend fun createMissing(userId: String, nameWasGiven: Boolean): ProfileEntity {
        val fallback = RemoteProfile(
            // Only a name she actually gave us. Seeding the row with the address-derived guess
            // would make that guess the account's real name on every other device she signs in on.
            displayName = session.displayName.value.takeIf { nameWasGiven },
            avatarUrl = null,
            partnerId = null,
            // Light, to match the column default set on 13 Aug 2026. This is an explicit INSERT, so
            // the default never applies — spelling "dark" here wrote a preference she never chose,
            // and iOS reads that column back on sign-in and honours it.
            theme = "light",
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
