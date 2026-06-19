package com.genesyx.app.domain.model

import java.time.LocalDateTime
import java.util.UUID

/** A single urine-pH reading. Mirrors `ph_readings` (see docs/DATA_LAYER.md ph.functions). */
data class PhReading(
    val id: String = UUID.randomUUID().toString(),
    val phValue: Double,
    val recordedAt: LocalDateTime,
    val notes: String? = null,
)
