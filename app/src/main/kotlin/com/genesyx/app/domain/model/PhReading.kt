package com.genesyx.app.domain.model

import java.time.LocalDateTime
import java.util.UUID

/** Measurement site for a pH reading. Stored so pre-migration urine readings stay distinguishable
 *  from vaginal ones — new writes are [VAGINAL]; rows logged before the 1.2.x switch are [URINE]. */
object PhMeasurement {
    const val URINE = "urine"
    const val VAGINAL = "vaginal"
}

/** A single vaginal-pH reading. Mirrors `ph_readings` (see docs/DATA_LAYER.md ph.functions).
 *  [measurementType] defaults to [PhMeasurement.VAGINAL] — every new reading is a vaginal measurement. */
data class PhReading(
    val id: String = UUID.randomUUID().toString(),
    val phValue: Double,
    val recordedAt: LocalDateTime,
    val notes: String? = null,
    val measurementType: String = PhMeasurement.VAGINAL,
)
