package com.genesyx.app.domain.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * The Learn tab's "12-week plan" card reports where the weekly programme actually is. It is the one
 * place in the app that talks about articles the reader cannot open yet, so it has to stay honest at
 * all three points in the run: before the first release, mid-run, and after the last one.
 */
class LearnSeriesTest {

    private val series = LearnDrip.weeklySeries
    private val firstDate = series.first().publishedAt!!
    private val lastDate = series.last().publishedAt!!

    @Test
    fun `the series is every dated article, in release order`() {
        assertTrue("The weekly programme should not be empty", series.isNotEmpty())
        assertTrue(
            "Every series article must carry a publish date",
            series.all { it.publishedAt != null },
        )
        // mapNotNull, not map: `map` yields List<LocalDate?>, and `sorted()` needs a non-nullable
        // Comparable. The assertion above has already established there are no nulls to drop.
        val dates = series.mapNotNull { it.publishedAt }
        // Plain two-arg form: assertEquals(String, Int, Int) is the one overload Kotlin can resolve
        // surprisingly (long vs boxed Object), and nothing else in this suite relies on it.
        assertEquals(series.size, dates.size)
        assertEquals("The series must be sorted by release date", dates.sorted(), dates)
        // Undated articles are the always-available library, never part of the programme.
        assertTrue(learnArticles.any { it.publishedAt == null })
    }

    @Test
    fun `before the first release nothing is out and the next date is the first one`() {
        val before = firstDate.minusDays(1)
        assertEquals(0, LearnDrip.seriesReleasedCount(before))
        assertNull(LearnDrip.latestSeriesArticle(before))
        assertEquals(firstDate, LearnDrip.nextSeriesDate(before))
    }

    @Test
    fun `on the first release day exactly one is out and it is the one to open`() {
        assertEquals(1, LearnDrip.seriesReleasedCount(firstDate))
        assertEquals(series.first().slug, LearnDrip.latestSeriesArticle(firstDate)?.slug)
    }

    @Test
    fun `after the last release everything is out and there is no next date`() {
        val after = lastDate.plusDays(1)
        assertEquals(series.size, LearnDrip.seriesReleasedCount(after))
        assertEquals(series.last().slug, LearnDrip.latestSeriesArticle(after)?.slug)
        assertNull(LearnDrip.nextSeriesDate(after))
    }

    @Test
    fun `the series entry point does not expire, unlike the new-this-week nudge`() {
        // newestReleased deliberately goes quiet a week after a release; the Learn card must not, or
        // the way into the programme disappears once the run is over. Measured from the LAST release
        // — releases sit a week apart, so any earlier vantage point still has a fresh one in window.
        val longAfterTheRun = lastDate.plusDays(10)
        assertNull(LearnDrip.newestReleased(longAfterTheRun))
        assertEquals(series.last().slug, LearnDrip.latestSeriesArticle(longAfterTheRun)?.slug)
    }

    @Test
    fun `every series article is hidden before its date and visible on it`() {
        series.forEach { a ->
            val date = a.publishedAt!!
            assertTrue(LearnDrip.isPublished(a, date))
            assertTrue(!LearnDrip.isPublished(a, date.minusDays(1)))
        }
    }
}
