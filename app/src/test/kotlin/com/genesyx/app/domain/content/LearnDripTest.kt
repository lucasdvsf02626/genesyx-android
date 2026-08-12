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

    @Test
    fun `published returns every always-available article and hides nothing spuriously`() {
        val published = LearnDrip.published(today)
        assertEquals(learnArticles.size, published.size)
        assertTrue(learnArticles.all { it in published })
    }

    @Test
    fun `no dated content has shipped yet, so nothing releases on a given day`() {
        assertTrue(learnArticles.all { it.publishedAt == null })
        assertTrue(LearnDrip.releasedOn(today).isEmpty())
        assertNull(LearnDrip.newestReleased(today))
    }
}
