package com.genesyx.app.domain.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class LearnNavigationTest {

    @Test
    fun `each tab help slug opens a live article`() {
        val today = LocalDate.of(2026, 8, 17)
        AppGuide.tabSignposts.forEach { slug ->
            assertNotNull("$slug must resolve", articleBySlug(slug))
            assertEquals(slug, LearnNavigation.publishedSlug(slug, today))
        }
    }

    @Test
    fun `a future-dated weekly article does not open`() {
        val beforeWeek3 = LocalDate.of(2026, 8, 17)
        val slug = "nutrition-before-conception"
        val article = requireNotNull(articleBySlug(slug))
        assertTrue("precondition: week 3 is dated after 17 Aug", article.publishedAt!!.isAfter(beforeWeek3))
        assertNull(
            "a future-dated slug must not open — it would look like a broken article",
            LearnNavigation.publishedSlug(slug, beforeWeek3),
        )
    }

    @Test
    fun `the same weekly article opens on its reveal day`() {
        val reveal = LocalDate.of(2026, 9, 6)
        assertEquals("nutrition-before-conception", LearnNavigation.publishedSlug("nutrition-before-conception", reveal))
    }

    @Test
    fun `an unknown slug does not open`() {
        assertNull(LearnNavigation.publishedSlug("not-a-real-slug", LocalDate.of(2026, 8, 17)))
    }

    @Test
    fun `ios shettles alias is gated until 8 Nov and then returns the canonical slug`() {
        assertNull(LearnNavigation.publishedSlug("shettles-method", LocalDate.of(2026, 11, 7)))
        assertEquals(
            "shettles-method-theory-vs-evidence",
            LearnNavigation.publishedSlug("shettles-method", LocalDate.of(2026, 11, 8)),
        )
    }

    @Test
    fun `new-article deep link names today's released slug`() {
        assertEquals("genesyx://learn", LearnNavigation.newArticleDeepLink(LocalDate.of(2026, 8, 17)))
        assertEquals(
            "genesyx://learn/article/fertile-window",
            LearnNavigation.newArticleDeepLink(LocalDate.of(2026, 8, 23)),
        )
    }
}
