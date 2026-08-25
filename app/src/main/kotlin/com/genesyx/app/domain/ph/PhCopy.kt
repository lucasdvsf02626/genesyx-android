package com.genesyx.app.domain.ph

import com.genesyx.app.domain.content.Citation

/**
 * All user-visible copy for the vaginal-pH tracker that carries health framing, in one place so it
 * can be scanned by the banned-phrase guard (see PhCopyBannedPhraseTest, which mirrors the Learn
 * guard). Copy is deliberately neutral and descriptive: it names no conditions, gives no dietary
 * advice, offers no diagnosis, and signposts a GP or pharmacist for persistently elevated readings.
 *
 * Client-approved for the 1.3.2 release alongside the range in [PhStatus].
 */
object PhCopy {
    /** Shown on the pH detail screen and the log dialog. */
    const val DISCLAIMER =
        "This tracker is for your own record and isn't medical advice. If a reading worries you, " +
            "or a pattern persists, please speak to a GP, nurse, or pharmacist."

    // The one canonical marker for a pre-migration reading. Rendered verbatim (lowercase) on
    // every surface — card pill, log-day row, Track summary, Home nudge — so casing never diverges.
    // Names the reading as off-scale without naming the sample type; the distinction it carries is
    // load-bearing, because these values are not on the vaginal scale and must never be classified.
    const val LEGACY_MARKER = "legacy reading"

    // One-time notice shown after the update that switched the tracker to vaginal pH.
    const val NOTICE_TITLE = "Vaginal pH tracking"
    const val NOTICE_BODY =
        "This tracker now records vaginal pH. Any readings you logged before this update are kept " +
            "and marked 'legacy reading'. New readings are saved as vaginal pH, on a different scale."
    const val NOTICE_DISMISS = "Got it"

    // Insight copy (two states). Default until there are enough recent readings.
    const val INSIGHT_DEFAULT = "Log a few more readings and we'll share gentle observations."
    const val INSIGHT_HEALTHY = "Your recent readings sit within the typical healthy range."
    const val INSIGHT_ELEVATED = "Your recent readings are above the typical healthy range."
    const val RECOMMENDATION_ELEVATED =
        "If readings stay above the usual range over several days, a GP or pharmacist can talk it " +
            "through with you."

    // ── The four questions the pH detail screen must answer ──────────────────────────────────────
    // Every factual claim below traces to one of [SOURCES]. Written to pass the banned-phrase guard:
    // no condition is named, nothing is described as treatable or curable, and no supplement, food or
    // product is suggested as a way to change a reading.

    const val WHY_TITLE = "Why vaginal pH matters"
    const val WHY_BODY =
        "During the reproductive years, lactobacilli in the vagina turn glycogen into lactic acid. " +
            "That keeps things acidic — usually between 3.8 and 4.5 — which helps the local flora " +
            "stay stable and makes it harder for other bacteria to overgrow. Your own usual number " +
            "is the useful thing to know here, not a target to hit."

    // Cautious background only: no causal claim, no score, no sex-selection. Production copy.
    const val FERTILITY_TITLE = "How this relates to fertility"
    const val FERTILITY_BODY =
        "A settled, acidic vaginal environment is part of general reproductive wellbeing, so some " +
            "people like to keep an eye on it while trying to conceive. Genesyx keeps this as " +
            "background context, not a fertility test — your pH does not score your chances, and one " +
            "reading tells you little on its own. If something feels off, a GP, nurse or pharmacist " +
            "is the right place to check."

    const val ACCURACY_TITLE = "For a reading you can trust"
    const val ACCURACY_BODY =
        "For an accurate reading, test away from your period and at least 24 hours after sex. " +
            "Lubricants, scented washes, and recent antibiotics can also shift the result."

    const val SUPPORT_TITLE = "Supporting your vaginal health"
    const val SUPPORT_BODY =
        "Your body keeps its own balance, and it usually does that best when left alone. Warm " +
            "water is enough for washing — the outside only, as the inside looks after itself. " +
            "Unscented products, breathable cotton underwear, and changing out of damp swimwear " +
            "or gym kit all help. Scented washes work against that balance rather than for it."
    const val SUPPORT_SIGNPOST =
        "If you notice a change in discharge, smell, or comfort that is new for you, a " +
            "pharmacist can talk it through — no appointment needed, and it is a very ordinary " +
            "thing to ask about."

    const val SEEK_HELP_TITLE = "When to ask for support"
    const val SEEK_HELP_BODY =
        "Speak to a GP, nurse, pharmacist, or a sexual health clinic if readings stay high over " +
            "several days, or if you notice a change in the colour, smell or texture of discharge, " +
            "itching or soreness, bleeding between periods or after sex, or pain when you pee. " +
            "This app cannot tell you what is causing a change."

    const val SHETTLES_TITLE = "The Shettles method"
    const val SHETTLES_BODY =
        "The Shettles method is a decades-old theory about timing sex and the sex of a baby. It " +
            "is not a proven method, and Genesyx does not use it to guide this tracker."
    const val SHETTLES_LINK = "Read the theory versus the evidence"

    const val MEANS_TITLE = "What your reading means"
    const val MEANS_NONE = "Log your first reading and this section will explain what it means."
    const val MEANS_HEALTHY =
        "This reading sits inside the range usually described as typical for the reproductive " +
            "years, 3.8 to 4.5. One number is only a snapshot — where your readings usually sit " +
            "tells you more than any single day."
    const val MEANS_ELEVATED =
        "This reading is above the typical 3.8 to 4.5 range. On its own that is not a cause for " +
            "alarm: pH shifts around your period, after sex, and with some washes and products. " +
            "What matters is whether it stays high across several days."

    const val DO_TITLE = "What to do next"
    const val DO_NONE =
        "Log a reading whenever it suits you. There is nothing you need to do to prepare."
    const val DO_HEALTHY =
        "Nothing needs doing. Keep logging when it suits you — a run of readings is far more " +
            "useful than any one of them. For everyday care the NHS suggests washing gently with " +
            "warm water and skipping perfumed products."
    const val DO_ELEVATED =
        "Keep watching rather than acting on a single number. Speak to a GP, nurse, pharmacist, or " +
            "a sexual health clinic if readings stay high over several days, or if you notice a " +
            "change in the colour, smell or texture of discharge, itching or soreness, bleeding " +
            "between periods or after sex, or pain when you pee."

    const val SUPPLEMENTS_TITLE = "How your Genesyx plan relates"
    const val SUPPLEMENTS_BODY =
        "Your supplement plan and this tracker sit side by side, and that is all. We make no claim " +
            "that any supplement changes your pH, and nothing here is a reason to start or stop " +
            "taking one. Logging both simply lets you see your routine and your readings in one place."
    const val SUPPLEMENTS_CTA = "Open your plan"

    const val SOURCES_TITLE = "Sources"

    /**
     * What the copy above is based on. Publisher titles and dates are quoted as published, so these
     * are deliberately NOT scanned by the banned-phrase guard — that guard governs claims Genesyx
     * writes, not the names of the sources it cites (see [all]).
     */
    val SOURCES: List<Citation> = listOf(
        Citation(
            title = "Physiology, Vaginal Structure and Function",
            publisher = "StatPearls, NCBI Bookshelf",
            reviewed = "5 July 2026",
            url = "https://www.ncbi.nlm.nih.gov/books/NBK545147/",
        ),
        Citation(
            title = "Vaginal discharge",
            publisher = "NHS",
            reviewed = "15 February 2024",
            url = "https://www.nhs.uk/symptoms/vaginal-discharge/",
        ),
    )

    /** Copy for the two informational sections that vary by band. */
    fun meansFor(status: PhStatus?): String = when (status) {
        null -> MEANS_NONE
        PhStatus.HEALTHY -> MEANS_HEALTHY
        PhStatus.ELEVATED -> MEANS_ELEVATED
    }

    fun doNextFor(status: PhStatus?): String = when (status) {
        null -> DO_NONE
        PhStatus.HEALTHY -> DO_HEALTHY
        PhStatus.ELEVATED -> DO_ELEVATED
    }

    /** Every health-framed string Genesyx authors, for the banned-phrase guard. */
    fun all(): List<String> = listOf(
        DISCLAIMER, LEGACY_MARKER, NOTICE_TITLE, NOTICE_BODY, NOTICE_DISMISS,
        INSIGHT_DEFAULT, INSIGHT_HEALTHY, INSIGHT_ELEVATED, RECOMMENDATION_ELEVATED,
        WHY_TITLE, WHY_BODY,
        FERTILITY_TITLE, FERTILITY_BODY,
        ACCURACY_TITLE, ACCURACY_BODY,
        SUPPORT_TITLE, SUPPORT_BODY, SUPPORT_SIGNPOST,
        SEEK_HELP_TITLE, SEEK_HELP_BODY,
        SHETTLES_TITLE, SHETTLES_BODY, SHETTLES_LINK,
        MEANS_TITLE, MEANS_NONE, MEANS_HEALTHY, MEANS_ELEVATED,
        DO_TITLE, DO_NONE, DO_HEALTHY, DO_ELEVATED,
        SUPPLEMENTS_TITLE, SUPPLEMENTS_BODY, SUPPLEMENTS_CTA, SOURCES_TITLE,
    )
}
