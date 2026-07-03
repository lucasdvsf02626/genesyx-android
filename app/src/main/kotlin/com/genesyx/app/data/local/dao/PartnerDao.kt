package com.genesyx.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.genesyx.app.data.local.entity.PartnerInviteEntity
import com.genesyx.app.data.local.entity.PartnerLinkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PartnerDao {
    @Query("SELECT * FROM partner_invites WHERE ownerUserId = :ownerUserId ORDER BY email COLLATE NOCASE ASC")
    fun observeInvites(ownerUserId: String): Flow<List<PartnerInviteEntity>>

    @Upsert
    suspend fun upsertInvite(entity: PartnerInviteEntity)

    @Query("SELECT * FROM partner_links WHERE ownerUserId = :ownerUserId LIMIT 1")
    fun observeLink(ownerUserId: String): Flow<PartnerLinkEntity?>

    @Upsert
    suspend fun upsertLink(entity: PartnerLinkEntity)

    @Query("DELETE FROM partner_links WHERE ownerUserId = :ownerUserId")
    suspend fun clearLink(ownerUserId: String)
}
