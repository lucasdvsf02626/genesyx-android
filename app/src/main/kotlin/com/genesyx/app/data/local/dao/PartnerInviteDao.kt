package com.genesyx.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.genesyx.app.data.local.entity.PartnerInviteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PartnerInviteDao {
    @Upsert suspend fun upsert(entity: PartnerInviteEntity)

    @Query("SELECT * FROM partner_invites WHERE inviterId = :inviterId ORDER BY rowid DESC")
    fun getByInviterId(inviterId: String): Flow<List<PartnerInviteEntity>>

    @Query("SELECT * FROM partner_invites WHERE code = :code LIMIT 1")
    suspend fun getByCode(code: String): PartnerInviteEntity?
}
