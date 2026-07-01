package com.genesyx.app.data.repository

import com.genesyx.app.data.local.dao.PartnerInviteDao
import com.genesyx.app.data.local.entity.PartnerInviteEntity
import com.genesyx.app.data.remote.PartnerInviteDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

interface PartnerRepository {
    fun getMyInvites(userId: String): Flow<List<PartnerInviteEntity>>
    suspend fun createInvite(inviterId: String, inviteeEmail: String): Result<String>
    suspend fun acceptInvite(code: String): Result<Unit>
    suspend fun unlinkPartner(): Result<Unit>
}

@Singleton
class SupabasePartnerRepository @Inject constructor(
    private val dao: PartnerInviteDao,
    private val supabase: SupabaseClient,
) : PartnerRepository {

    override fun getMyInvites(userId: String): Flow<List<PartnerInviteEntity>> =
        dao.getByInviterId(userId)

    override suspend fun createInvite(inviterId: String, inviteeEmail: String): Result<String> =
        runCatching {
            val code = UUID.randomUUID().toString().replace("-", "").take(16)
            val dto = PartnerInviteDto(
                id = UUID.randomUUID().toString(),
                inviterId = inviterId,
                inviteeEmail = inviteeEmail,
                code = code,
                status = "pending",
                expiresAt = Instant.now().plusSeconds(14 * 24 * 3600).toString(),
            )
            supabase.postgrest["partner_invites"].insert(dto)
            dao.upsert(dto.toEntity())
            code
        }

    override suspend fun acceptInvite(code: String): Result<Unit> = runCatching {
        supabase.functions.invoke(
            "partner-accept",
            body = buildJsonObject { put("code", code) },
        )
    }

    override suspend fun unlinkPartner(): Result<Unit> = runCatching {
        supabase.functions.invoke("partner-unlink", body = buildJsonObject { })
    }

    private fun PartnerInviteDto.toEntity() = PartnerInviteEntity(
        id = id,
        inviterId = inviterId,
        inviteeEmail = inviteeEmail,
        code = code,
        status = status,
        expiresAt = Instant.parse(expiresAt),
        acceptedBy = acceptedBy,
        acceptedAt = acceptedAt?.let { Instant.parse(it) },
    )
}
