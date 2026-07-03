package com.genesyx.app.data.remote

import com.genesyx.app.core.log.Logger
import com.genesyx.app.core.result.DataResult
import com.genesyx.app.data.remote.dto.ProfileDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real Supabase (`profiles` table) implementation of [ProfileRemoteDataSource]. RLS scopes every row
 * to `auth.uid()`, so the signed-in session's JWT (held by supabase-kt) determines what is visible.
 * Bound only when creds are configured (see NetworkModule); otherwise the local-first stub is used.
 */
@Singleton
class SupabaseProfileRemoteDataSource @Inject constructor(
    private val client: SupabaseClient,
    private val logger: Logger,
) : ProfileRemoteDataSource {

    override suspend fun getProfile(userId: String): DataResult<RemoteProfile?> =
        try {
            val dto = client.from("profiles")
                .select { filter { eq("id", userId) } }
                .decodeSingleOrNull<ProfileDto>()
            DataResult.Success(dto?.toRemote())
        } catch (t: Throwable) {
            logger.e("Profile", "getProfile failed", t)
            DataResult.Error(t, t.message)
        }

    override suspend fun upsertProfile(userId: String, profile: RemoteProfile): DataResult<Unit> =
        try {
            client.from("profiles").upsert(
                ProfileDto(
                    id = userId,
                    displayName = profile.displayName,
                    avatarUrl = profile.avatarUrl,
                    partnerId = profile.partnerId,
                    theme = profile.theme,
                ),
            )
            DataResult.Success(Unit)
        } catch (t: Throwable) {
            logger.e("Profile", "upsertProfile failed", t)
            DataResult.Error(t, t.message)
        }

    override suspend fun updateDisplayName(userId: String, name: String): DataResult<Unit> =
        try {
            client.from("profiles").update({ set("display_name", name) }) { filter { eq("id", userId) } }
            DataResult.Success(Unit)
        } catch (t: Throwable) {
            logger.e("Profile", "updateDisplayName failed", t)
            DataResult.Error(t, t.message)
        }

    override suspend fun updateTheme(userId: String, theme: String): DataResult<Unit> =
        try {
            client.from("profiles").update({ set("theme", theme) }) { filter { eq("id", userId) } }
            DataResult.Success(Unit)
        } catch (t: Throwable) {
            logger.e("Profile", "updateTheme failed", t)
            DataResult.Error(t, t.message)
        }

    private fun ProfileDto.toRemote() = RemoteProfile(
        displayName = displayName,
        avatarUrl = avatarUrl,
        partnerId = partnerId,
        theme = theme,
    )
}
