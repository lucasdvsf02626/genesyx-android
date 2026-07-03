package com.genesyx.app.data

import com.genesyx.app.core.di.ApplicationScope
import com.genesyx.app.data.local.dao.PartnerDao
import com.genesyx.app.data.local.entity.PartnerLinkEntity
import com.genesyx.app.data.local.entity.toDomain
import com.genesyx.app.data.local.entity.toEntity
import com.genesyx.app.domain.model.InviteStatus
import com.genesyx.app.domain.model.Partner
import com.genesyx.app.domain.model.PartnerInvite
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Partner linking, now persisted in Room (scoped per user) instead of memory — invites and the linked
 * partner survive restart. Public API is unchanged (invites/partner StateFlows + send/revoke/accept/
 * unlink) so Profile is untouched. Mirrors Supabase `partner_invites` / `profiles.partner_id`
 * (docs/DATA_LAYER.md). Privileged accept/unlink become Supabase Edge Functions once wired; for now
 * `accept` links a local placeholder partner, matching prior behaviour.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class PartnerRepository @Inject constructor(
    private val dao: PartnerDao,
    private val session: SessionRepository,
    @ApplicationScope private val scope: CoroutineScope,
) {
    val invites: StateFlow<List<PartnerInvite>> =
        session.userId
            .flatMapLatest { uid -> dao.observeInvites(uid ?: SessionRepository.LOCAL_USER_ID) }
            .map { list -> list.map { it.toDomain() } }
            .stateIn(scope, SharingStarted.Eagerly, emptyList())

    val partner: StateFlow<Partner?> =
        session.userId
            .flatMapLatest { uid -> dao.observeLink(uid ?: SessionRepository.LOCAL_USER_ID) }
            .map { link -> link?.let { Partner(it.partnerName) } }
            .stateIn(scope, SharingStarted.Eagerly, null)

    fun sendInvite(email: String) {
        val code = UUID.randomUUID().toString().replace("-", "").take(16)
        val invite = PartnerInvite(id = UUID.randomUUID().toString(), email = email, code = code)
        scope.launch { dao.upsertInvite(invite.toEntity(session.currentUserId())) }
    }

    fun revoke(id: String) {
        scope.launch {
            invites.value.firstOrNull { it.id == id }?.let {
                dao.upsertInvite(it.copy(status = InviteStatus.REVOKED).toEntity(session.currentUserId()))
            }
        }
    }

    fun accept(@Suppress("UNUSED_PARAMETER") code: String) {
        scope.launch { dao.upsertLink(PartnerLinkEntity(session.currentUserId(), "Your partner")) }
    }

    fun unlink() {
        scope.launch { dao.clearLink(session.currentUserId()) }
    }
}
