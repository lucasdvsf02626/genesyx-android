package com.genesyx.app.core

/** Compile-time feature gates. A disabled feature is fully hidden from the UI. */
object FeatureFlags {
    /**
     * Urine-pH tracking. Disabled for the 1.0 release — the backend sync path is not yet verified,
     * so the whole feature (tracker card, log dialog, insights section) is hidden.
     */
    const val PH_TRACKING = false
}
