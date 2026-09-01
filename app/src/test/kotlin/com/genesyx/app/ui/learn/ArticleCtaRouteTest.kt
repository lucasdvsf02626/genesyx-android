package com.genesyx.app.ui.learn

import com.genesyx.app.domain.content.AppGuide
import com.genesyx.app.domain.content.ArticleCta
import com.genesyx.app.domain.content.CtaType
import com.genesyx.app.domain.content.LearnDrip
import com.genesyx.app.domain.content.articleBySlug
import com.genesyx.app.domain.content.learnArticles
import com.genesyx.app.ui.navigation.Screen
import com.genesyx.app.ui.navigation.TabNavigation
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The article CTA's routing contract — the "See your insights" report (1 Sep 2026).
 *
 * The button was not dead: it navigated to `insights` with `popUpTo(Home) { saveState = true }` +
 * `restoreState`, which saved the `insights → article` chain it was standing in and restored that
 * same chain, putting the article straight back. Nothing moved. It only bit where the article had
 * been opened *from* the tab its CTA points at, which is exactly how the tab signpost articles are
 * read: `reading-your-trends` is the Insights tab's own "how this works" link.
 *
 * The fix routes tab-bound CTAs through `navigateToTab` (SFM-27's rule), which pops back to the tab
 * root when it is the one beneath us. This pins the half of it that is testable off-device: which
 * CTAs resolve to a tab, and therefore take that path at all. `ArticleCtaTabNavigationTest`
 * (instrumented) walks the real back stack.
 */
class ArticleCtaRouteTest {

    @Test
    fun `every tab-bound CTA resolves to the tab it names`() {
        mapOf(
            CtaType.OPEN_TRACK to Screen.Track,
            CtaType.OPEN_PH to Screen.PhDetail,
            CtaType.OPEN_NUTRITION to Screen.Nutrition,
            CtaType.OPEN_INSIGHTS to Screen.Insights,
        ).forEach { (type, tab) ->
            val route = ArticleCta(type, "label").route()
            assertEquals("$type must route through navigateToTab", tab, TabNavigation.tabForRoute(route))
            assertFalse("$type navigates to a pattern: $route", route.contains("{"))
        }
    }

    @Test
    fun `the log and follow-on-article CTAs stay pushed destinations`() {
        // Neither is a tab: they keep their plain push, with Back returning to the article.
        assertNull(TabNavigation.tabForRoute(ArticleCta(CtaType.OPEN_LOG, "Open today's log").route()))
        assertNull(
            TabNavigation.tabForRoute(
                ArticleCta(CtaType.OPEN_ARTICLE, "Read next", targetSlug = AppGuide.INSIGHTS).route(),
            ),
        )
    }

    @Test
    fun `the article the reported button lives in still points at the Insights tab`() {
        // `reading-your-trends` is AppGuide.INSIGHTS — the article the Insights tab links to, and
        // the one in the bug report. Reached from Insights, its CTA points back at Insights: the
        // exact shape that used to restore the article instead of leaving it.
        val guide = requireNotNull(articleBySlug(AppGuide.INSIGHTS)) { "AppGuide.INSIGHTS names no article" }
        assertTrue("the Insights signpost must not be drip-gated", LearnDrip.isPublished(guide, LocalDate.now()))
        val cta = requireNotNull(guide.cta) { "${guide.slug} lost its CTA" }
        assertEquals("See your insights", cta.label)
        assertEquals(CtaType.OPEN_INSIGHTS, cta.type)
        assertEquals(Screen.Insights, TabNavigation.tabForRoute(cta.route()))
    }

    @Test
    fun `every tab signpost article with a tab CTA takes the navigateToTab path`() {
        // The dead-button family: a tab's "how this works" article is opened from that tab, so any
        // CTA of its own that targets a tab hits the save-and-restore trap. Today that is Insights
        // (`reading-your-trends`) and pH (`guide-understanding-vaginal-ph`). Whatever the set grows
        // to, each one has to resolve to a tab so the screen sends it through `navigateToTab`.
        val pushedTypes = setOf(CtaType.OPEN_LOG, CtaType.OPEN_ARTICLE)
        val signpostCtas = AppGuide.tabSignposts
            .mapNotNull { slug -> articleBySlug(slug) }
            .mapNotNull { article -> article.cta?.let { article.slug to it } }
            .filter { (_, cta) -> cta.type !in pushedTypes }
        assertTrue("expected signpost articles with tab CTAs", signpostCtas.isNotEmpty())
        signpostCtas.forEach { (slug, cta) ->
            assertNotNull("$slug CTA must resolve to a tab", TabNavigation.tabForRoute(cta.route()))
        }
    }

    @Test
    fun `no article CTA navigates to a route with an unfilled argument`() {
        learnArticles.mapNotNull { it.cta }.forEach { cta ->
            val route = cta.route()
            assertFalse("$route still carries a placeholder", route.contains("{"))
            assertTrue(route.isNotBlank())
        }
    }
}
