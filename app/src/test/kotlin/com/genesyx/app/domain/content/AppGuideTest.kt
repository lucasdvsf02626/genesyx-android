package com.genesyx.app.domain.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class AppGuideTest {

    @Test
    fun `every how-to slug is always published and never date-gated`() {
        AppGuide.entries.forEach { entry ->
            val article = articleBySlug(entry.slug)
            assertNotNull("${entry.slug} is missing from the library", article)
            assertNull(
                "${entry.slug} is date-gated — it would open as unavailable from How to use",
                article!!.publishedAt,
            )
        }
    }

    @Test
    fun `hub routes do not collide with an article slug`() {
        assertNull(articleBySlug(LearnHubRoutes.HOW_TO_USE))
        assertNull(articleBySlug(LearnHubRoutes.TWELVE_WEEK))
        assertTrue(LearnHubRoutes.HOW_TO_USE != LearnHubRoutes.TWELVE_WEEK)
    }

    @Test
    fun `the twelve-week list names every dated article in calendar order`() {
        val series = LearnDrip.weeklySeries
        assertEquals(12, series.size)
        assertEquals(series.map { it.publishedAt }, series.map { it.publishedAt }.sortedBy { it })
        assertEquals("fertile-window", series.first().slug)
        assertEquals(LocalDate.of(2026, 8, 23), series.first().publishedAt)
        assertEquals("nutrition-before-conception", series[2].slug)
        assertEquals("shettles-method-theory-vs-evidence", series.last().slug)
        assertEquals(LocalDate.of(2026, 11, 8), series.last().publishedAt)
    }
}
