package com.genesyx.app.ui.profile

import com.genesyx.app.core.result.SaveOutcome
import com.genesyx.app.data.PhWriteResult

/**
 * What a Profile editor needs to know about its own save: whether one is in flight, why the last
 * one didn't land, and whether it did — [saved] is the dialog's cue to close, which is why closing
 * is no longer something the button does on its own.
 *
 * One per section, so an error from the cycle editor can't surface over the tracking one.
 */
data class SaveState(
    val saving: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false,
)

/**
 * The local write landed in every failure case here — these repositories write to Room first — so
 * the copy must not imply the edit was lost. It also must not promise a retry: cycle settings have
 * no sync queue, so "we'll send it later" would be untrue.
 */
private const val UNREACHABLE =
    "Saved on this device, but we couldn't reach the server. Try again when you're back online."

private const val REFUSED =
    "Health data collection is off, so this wasn't saved. Turn it back on under Health data consent."

fun SaveOutcome.toSaveState(): SaveState = when (this) {
    SaveOutcome.Saved -> SaveState(saved = true)
    SaveOutcome.Refused -> SaveState(error = REFUSED)
    is SaveOutcome.Failed -> SaveState(error = UNREACHABLE)
}

/**
 * pH keeps its own result type because it has a third case the others don't: a value outside the
 * trackable range. Unlike the two above, a refused pH write persisted nothing at all, so the copy
 * must not say it was saved anywhere.
 */
fun PhWriteResult.toSaveState(): SaveState = when (this) {
    PhWriteResult.Accepted -> SaveState(saved = true)
    PhWriteResult.Refused -> SaveState(error = REFUSED)
    is PhWriteResult.OutOfRange -> SaveState(error = "Enter a pH between $min and $max.")
}
