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
