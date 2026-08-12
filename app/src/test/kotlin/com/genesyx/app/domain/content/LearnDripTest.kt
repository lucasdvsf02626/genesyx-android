package com.genesyx.app.domain.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * The date-based weekly reveal (cross-platform contract with iOS, 12 Aug 2026). [isPublished] is the
 * core gate; the list helpers read the real [learnArticles], every one of which is currently
 * always-available, so those assertions also pin the "no dated content shipped yet" state — when the
 * weekly series lands, these update alongside it.
 */
class LearnDripTest {

    private val today = LocalDate.of(2026, 8, 12)

    private fun dated(at: LocalDate?): Article = Article(
        id = "t",
        slug = "t",
        title = "t",
        excerpt = "t",
        body = listOf(ArticleBlock.Paragraph("t")),
        category = ArticleCategory.GUIDES,
        tags = listOf("t"),
        readingTime = "1 min read",
        publishedAt = at,
    )

    @Test
    fun `a null date is always published`() {
        assertTrue(LearnDrip.isPublished(dated(null), today))
    }

    @Test
    fun `a past or same-day date is published, a future one is not`() {
        assertTrue(LearnDrip.isPublished(dated(today.minusDays(1)), today))
        assertTrue("the reveal day itself counts", LearnDrip.isPublished(dated(today), today))
        assertFalse(LearnDrip.isPublished(dated(today.plusDays(1)), today))
    }

    // Real weekly-series anchors (ported from iOS 1.2.0 (18)).
    private val firstWeekly = LocalDate.of(2026, 8, 23)  // "fertile-window"
    private val secondWeekly = LocalDate.of(2026, 8, 30) // "vaginal-ph-explained"

    @Test
    fun `before the series starts, only always-available articles are published`() {
        val before = LocalDate.of(2026, 8, 1)
        val published = LearnDrip.published(before)
        assertTrue("every always-available article shows", learnArticles.filter { it.publishedAt == null }.all { it in published })
        assertTrue("no dated article leaks early", published.none { it.publishedAt != null })
        assertNull(LearnDrip.newestReleased(before))
        assertTrue(LearnDrip.releasedOn(before).isEmpty())
    }

    @Test
    fun `a dated article appears on its date and not before`() {
        val dayBefore = firstWeekly.minusDays(1)
        val fertile = articleBySlug("fertile-window")!!
        assertFalse(LearnDrip.isPublished(fertile, dayBefore))
        assertTrue(LearnDrip.isPublished(fertile, firstWeekly))
        assertTrue("hidden the day before", articleBySlug("fertile-window") !in LearnDrip.published(dayBefore))
    }

    @Test
    fun `releasedOn and newestReleased key off the exact publish date`() {
        assertEquals(listOf("vaginal-ph-explained"), LearnDrip.releasedOn(secondWeekly).map { it.slug })
        assertEquals("vaginal-ph-explained", LearnDrip.newestReleased(secondWeekly)?.slug)
        // A week after the LAST release (the Shettles piece, 2026-11-08), nothing counts as "new" —
        // the consecutive-Sunday cadence means every earlier date still has a release within 7 days.
        assertNull(LearnDrip.newestReleased(LocalDate.of(2026, 11, 15)))
    }
}
