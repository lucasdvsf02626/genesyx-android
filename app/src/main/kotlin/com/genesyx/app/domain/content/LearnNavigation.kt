package com.genesyx.app.domain.content

import java.time.LocalDate

/**
 * Resolves a Learn deep link. The only legal destination is a published article.
 *
 * A slug that compiles, or that will be live next Sunday, must not open. The article screen
 * already treats unpublished as "not found"; this gate stops the tabs from sending her there
 * in the first place. Returns the slug when it is live; null otherwise.
 */
object LearnNavigation {
    /** Canonical published slug, or null. Aliases (e.g. iOS `shettles-method`) resolve first. */
    fun publishedSlug(slug: String, today: LocalDate = LocalDate.now()): String? {
        val article = articleBySlug(slug) ?: return null
        return article.slug.takeIf { LearnDrip.isPublished(article, today) }
    }

    /**
     * Tap target for the weekly Learn reminder. Names today's released article when one exists;
     * otherwise the Learn tab. A future-dated slug must never appear here.
     */
    fun newArticleDeepLink(today: LocalDate = LocalDate.now()): String {
        val slug = LearnDrip.releasedOn(today).firstOrNull()?.slug ?: return "genesyx://learn"
        return "genesyx://learn/article/$slug"
    }
}
