package com.genesyx.app.domain.content

/**
 * A source behind a piece of health copy. Genesyx is a wellness app, not a medical device — the
 * standard we hold ourselves to is that every factual health claim in the product can be traced to a
 * named, dated, publicly reachable source the reader can open for herself.
 *
 * Deliberately a plain data class with no network or storage behind it: sources are compile-time
 * constants that ship with the binary, so a citation can never fail to load or drift from the copy it
 * supports. [reviewed] is the publisher's own last-reviewed/updated date, not the date we read it.
 */
data class Citation(
    val id: String = "",
    val title: String,
    val publisher: String,
    val reviewed: String,
    val url: String,
) {
    /** Single-line attribution, e.g. "NHS · Vaginal discharge · reviewed 15 February 2024". */
    val line: String
        get() = if (reviewed.isBlank()) "$publisher · $title"
        else "$publisher · $title · reviewed $reviewed"
}
