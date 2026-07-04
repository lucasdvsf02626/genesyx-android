package com.genesyx.app.core

/** Compile-time feature gates. A disabled feature is fully hidden from the UI. */
object FeatureFlags {
    /**
     * Urine-pH tracking. Disabled for the 1.0 release — the backend sync path is not yet verified,
     * so the whole feature (tracker card, log dialog, insights section) is hidden.
     */
    const val PH_TRACKING = false

    /**
     * Admin/dev "Manage clients" screen (add client, "Seed 100 demo clients" scale-test action).
     * Not a 1.0 user feature — disabled so the screen and its seed action are unreachable in
     * release. Code kept dormant (like [PH_TRACKING]); flip to re-enable.
     */
    const val ADMIN_CLIENTS = false
}
