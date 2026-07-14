package com.genesyx.app.core

/** Compile-time feature gates. A disabled feature is fully hidden from the UI. */
object FeatureFlags {
    /**
     * Urine-pH tracking. Room is the source of truth and, for a signed-in user,
     * [com.genesyx.app.data.PhRepository] write-throughs to the Supabase `ph_readings` table (with a
     * WorkManager retry queue). Guests (`LOCAL_USER_ID`) stay on-device. The card copy must keep
     * saying so — pH is intimate health data and the sync has to be disclosed, not buried.
     */
    const val PH_TRACKING = true

    /**
     * Admin/dev "Manage clients" screen (add client, "Seed 100 demo clients" scale-test action).
     * Not a 1.0 user feature — disabled so the screen and its seed action are unreachable in
     * release. Code kept dormant (like [PH_TRACKING]); flip to re-enable.
     */
    const val ADMIN_CLIENTS = false

    /**
     * "Add your partner" invite/link section on Profile. Disabled for 1.0: the flow is UI-only —
     * [com.genesyx.app.data.PartnerRepository.sendInvite] writes a local Room row and sends NO email,
     * and accept/link is a local placeholder (no real cross-account linking). Hidden until the
     * Supabase `partner_invites` table + an invite-email Edge Function land in v1.1. Code kept
     * dormant (like [ADMIN_CLIENTS]); flip to re-enable.
     */
    const val PARTNER_INVITES = false

    /**
     * "Push Notifications" toggle on Profile. Disabled for 1.0: it is UI-only — the switch persists a
     * `push_enabled` boolean but nothing consumes it. There is NO notification infrastructure (no FCM/
     * Firebase, no NotificationManager/channel, no POST_NOTIFICATIONS permission, no reminder
     * scheduling), so toggling it does nothing. Hidden until notifications land in v1.1. Code kept
     * dormant (like [PARTNER_INVITES]); flip to re-enable.
     */
    const val PUSH_NOTIFICATIONS = false
}
