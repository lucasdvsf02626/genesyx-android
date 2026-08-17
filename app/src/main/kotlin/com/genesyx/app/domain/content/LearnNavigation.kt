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
    fun publishedSlug(slug: String, today: LocalDate = LocalDate.now()): String? {
        val article = articleBySlug(slug) ?: return null
        return slug.takeIf { LearnDrip.isPublished(article, today) }
    }
}
