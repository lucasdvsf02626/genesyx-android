package com.genesyx.app.domain.model

/** Current-focus toggle on Profile (fertility prep vs pregnancy mode). */
enum class FocusMode { PREP, PREGNANCY }

/** App theme preference; SYSTEM follows the device setting. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class InviteStatus { PENDING, ACCEPTED, REVOKED }

/** A partner invite the current user has sent. Mirrors `partner_invites`. */
data class PartnerInvite(
    val id: String,
    val email: String,
    val code: String,
    val status: InviteStatus = InviteStatus.PENDING,
)

/** A linked partner. */
data class Partner(val name: String)
