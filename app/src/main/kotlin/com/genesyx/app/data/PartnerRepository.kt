package com.genesyx.app.data

import com.genesyx.app.domain.model.InviteStatus
import com.genesyx.app.domain.model.Partner
import com.genesyx.app.domain.model.PartnerInvite
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory partner linking. Stands in for the partner server fns (docs/DATA_LAYER.md
 * partner.functions) — `sendInvite` generates a 16-char code, and `accept` links a mock partner.
 */
@Singleton
class PartnerRepository @Inject constructor() {

    private val _invites = MutableStateFlow<List<PartnerInvite>>(emptyList())
    val invites: StateFlow<List<PartnerInvite>> = _invites.asStateFlow()

    private val _partner = MutableStateFlow<Partner?>(null)
    val partner: StateFlow<Partner?> = _partner.asStateFlow()

    fun sendInvite(email: String) {
        val code = UUID.randomUUID().toString().replace("-", "").take(16)
        _invites.value = _invites.value + PartnerInvite(UUID.randomUUID().toString(), email, code)
    }

    fun revoke(id: String) {
        _invites.value = _invites.value.map {
            if (it.id == id) it.copy(status = InviteStatus.REVOKED) else it
        }
    }

    fun accept(@Suppress("UNUSED_PARAMETER") code: String) {
        _partner.value = Partner("Your partner")
    }

    fun unlink() {
        _partner.value = null
    }
}
