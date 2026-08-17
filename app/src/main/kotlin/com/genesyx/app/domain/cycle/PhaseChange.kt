package com.genesyx.app.domain.cycle

import com.genesyx.app.domain.model.Phase
import java.time.LocalDate

/**
 * Whether Home should treat today as the first day of a new phase (a phase-entry moment),
 * rather than the always-on cycle card.
 */
object PhaseChange {
    data class Decision(
        val justChanged: Boolean,
        val persistPhase: String?,
        val persistEpochDay: Long?,
    )

    fun evaluate(
        current: Phase?,
        storedPhase: String?,
        storedEpochDay: Long?,
        today: LocalDate,
    ): Decision {
        if (current == null) return Decision(justChanged = false, persistPhase = null, persistEpochDay = null)
        val name = current.name
        val todayEpoch = today.toEpochDay()
        return when {
            storedPhase == null ->
                Decision(justChanged = false, persistPhase = name, persistEpochDay = todayEpoch)
            storedPhase != name ->
                Decision(justChanged = true, persistPhase = name, persistEpochDay = todayEpoch)
            storedEpochDay == todayEpoch ->
                Decision(justChanged = true, persistPhase = null, persistEpochDay = null)
            else ->
                Decision(justChanged = false, persistPhase = null, persistEpochDay = null)
        }
    }
}
