package com.genesyx.app.ui.nutrition

import com.genesyx.app.domain.model.SupplementPlanEntry

/**
 * What a tap on a plan chip needs to tell her when it did not simply save. A `Saved` outcome is
 * silent — the chip filling is the confirmation — so there is no event for it.
 */
sealed interface SupplementSaveEvent {
    val entry: SupplementPlanEntry

    /** Health-data consent is withdrawn; nothing was written and the chip does not change. */
    data class Refused(override val entry: SupplementPlanEntry) : SupplementSaveEvent

    /** Written on the device; the server push failed and is queued for retry. */
    data class Queued(override val entry: SupplementPlanEntry) : SupplementSaveEvent

    /** The local write itself failed; nothing changed. Offer a retry. */
    data class Failed(override val entry: SupplementPlanEntry, val message: String?) : SupplementSaveEvent

    val text: String
        get() = when (this) {
            is Refused ->
                "Not saved — health data collection is off. Turn it on under Profile → Health data consent."
            is Queued -> "${entry.display} saved on this device — it'll sync when you're back online."
            is Failed -> "Couldn't save ${entry.display}. Nothing was changed."
        }
}
