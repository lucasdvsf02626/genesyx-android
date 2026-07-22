package com.genesyx.app.domain.ph

/**
 * All user-visible copy for the vaginal-pH tracker that carries health framing, in one place so it
 * can be scanned by the banned-phrase guard (see PhCopyBannedPhraseTest, which mirrors the Learn
 * guard). Copy is deliberately neutral and descriptive: it names no conditions, gives no dietary
 * advice, offers no diagnosis, and signposts a GP or pharmacist for persistently elevated readings.
 *
 * ⚠️ PROVISIONAL — the wording accompanies the provisional ranges in [PhStatus] and is subject to
 * the same clinical sign-off before release.
 */
object PhCopy {
    /** Shown on the pH detail screen and the log dialog. */
    const val DISCLAIMER =
        "This tracker is for your own record and isn't medical advice. If a reading worries you, " +
            "or a pattern persists, please speak to a GP, nurse, or pharmacist."

    // The one canonical marker for a pre-migration urine reading. Rendered verbatim (lowercase) on
    // every surface — card pill, log-day row, Track summary, Home nudge — so casing never diverges.
    const val LEGACY_MARKER = "urine (legacy)"

    // One-time notice shown after the update that switched the tracker to vaginal pH.
    const val NOTICE_TITLE = "Vaginal pH tracking"
    const val NOTICE_BODY =
        "This tracker now records vaginal pH. Any readings you logged before this update are kept " +
            "and marked 'urine (legacy)'. New readings are saved as vaginal pH, on a different scale."
    const val NOTICE_DISMISS = "Got it"

    // Insight copy (two states). Default until there are enough recent readings.
    const val INSIGHT_DEFAULT = "Log a few more readings and we'll share gentle observations."
    const val INSIGHT_HEALTHY = "Your recent readings sit within the typical healthy range."
    const val INSIGHT_ELEVATED = "Your recent readings are above the typical healthy range."
    const val RECOMMENDATION_ELEVATED =
        "If readings stay above the usual range over several days, a GP or pharmacist can talk it " +
            "through with you."

    /** Every health-framed string here, for the banned-phrase guard. */
    fun all(): List<String> = listOf(
        DISCLAIMER, LEGACY_MARKER, NOTICE_TITLE, NOTICE_BODY, NOTICE_DISMISS,
        INSIGHT_DEFAULT, INSIGHT_HEALTHY, INSIGHT_ELEVATED, RECOMMENDATION_ELEVATED,
    )
}
