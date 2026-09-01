package com.genesyx.app.domain.consent

import java.time.LocalDateTime

/**
 * One entry in the health-data consent trail.
 *
 * Consent is modelled as an append-only log rather than a boolean because the trail *is* the
 * evidence. A withdrawal that overwrote a grant would destroy the proof that the grant ever
 * happened, which is the one thing this record exists to demonstrate. Nothing in the app updates or
 * deletes an event; state is always derived by reading the trail.
 */
data class ConsentEvent(
    val id: String,
    val action: ConsentAction,
    val recordedAt: LocalDateTime,
)

enum class ConsentAction(val wire: String) {
    GRANTED("granted"),
    WITHDRAWN("withdrawn"),
    ;

    companion object {
        fun fromWire(value: String): ConsentAction =
            entries.firstOrNull { it.wire == value } ?: GRANTED
    }
}

/**
 * The consent-copy version stamped onto every pushed event (`consent_events.version` is
 * NOT NULL). MIRRORS iOS `ConsentPolicy.currentVersion` (GenesyxCore/Consent/Consent.swift) —
 * the two platforms show materially the same disclosure, so they share one version string, and
 * a copy change that bumps it on one platform must bump it on both.
 */
object ConsentPolicy {
    const val WIRE_VERSION = "2026-08-18.v2"
}
