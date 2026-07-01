package com.genesyx.app.data.repository

import com.genesyx.app.data.local.dao.ProfileDao
import com.genesyx.app.data.local.entity.ProfileEntity
import com.genesyx.app.data.remote.ProfileDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface ProfileRepository {
    fun getProfile(userId: String): Flow<ProfileEntity?>
    suspend fun upsertProfile(entity: ProfileEntity)
    suspend fun syncFromRemote(userId: String)
}

@Singleton
class SupabaseProfileRepository @Inject constructor(
    private val dao: ProfileDao,
    private val supabase: SupabaseClient,
) : ProfileRepository {

    override fun getProfile(userId: String): Flow<ProfileEntity?> = dao.getById(userId)

    override suspend fun upsertProfile(entity: ProfileEntity) {
        dao.upsert(entity)
        runCatching {
            supabase.postgrest["profiles"].upsert(
                ProfileDto(
                    id = entity.id,
                    displayName = entity.displayName,
                    avatarUrl = entity.avatarUrl,
                    partnerId = entity.partnerId,
                    theme = entity.theme,
                )
            )
        }
    }

    override suspend fun syncFromRemote(userId: String) {
        runCatching {
            val dto = supabase.postgrest["profiles"]
                .select { filter { eq("id", userId) } }
                .decodeSingle<ProfileDto>()
            dao.upsert(
                ProfileEntity(
                    id = dto.id,
                    displayName = dto.displayName,
                    avatarUrl = dto.avatarUrl,
                    partnerId = dto.partnerId,
                    theme = dto.theme,
                )
            )
        }
    }
}
