package com.genesyx.app.core.result

/**
 * What happened to a save the user initiated. Three cases, not two.
 *
 * [Refused] and [Failed] are deliberately separate: a write the Article 9 gate refused is not a
 * write that broke, and the remedies are nothing alike — one is a switch in Profile, the other is
 * waiting for a network. Telling her "couldn't reach the server" after she withdrew consent sends
 * her to fix something that isn't wrong. iOS collapses both into `false` because its sheet API made
 * that cheap; there is no reason to inherit that here.
 *
 * Distinct from [DataResult], which models a fetch that may still be in flight. A save the user
 * pressed a button for is settled by the time it returns, so there is no Loading case.
 */
sealed interface SaveOutcome {
    /** It landed. Local write done, and the server took it (or there was no server to take it). */
    data object Saved : SaveOutcome

    /** The health-data consent gate refused. Nothing was written; the remedy is in Profile. */
    data object Refused : SaveOutcome

    /**
     * The local write landed but the server rejected it or was unreachable. [message] is the
     * underlying reason where there is one — never shown raw, it only picks the copy.
     */
    data class Failed(val message: String?) : SaveOutcome
}
