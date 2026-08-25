package com.genesyx.app.domain.consent

/**
 * The lawful-basis check that guards every Article 9 health-data path — cycle settings, daily logs,
 * pH readings and tracking-preference answers.
 *
 * It is **suspending on purpose**. A synchronous gate would have to read a cached flag, and on a
 * cold start that cache is seeded before Room has emitted — so a write fired in the first moments
 * after launch would be waved through under a consent that had already been withdrawn. Suspending
 * lets the gate read the trail itself, and every call site is already inside a coroutine.
 *
 * Implementations default to permitted when the trail is empty. That is the iOS default too, and it
 * matters on upgrade: an existing user who has never been shown a consent screen must not have her
 * tracking silently stop.
 */
fun interface HealthDataCollectionGate {
    suspend fun isCollectionPermitted(): Boolean
}
