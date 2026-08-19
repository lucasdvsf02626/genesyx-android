package com.genesyx.app.domain.content

import com.genesyx.app.core.AppLinks
import com.genesyx.app.core.FeatureFlags
import com.genesyx.app.domain.ph.PhCopy
import com.genesyx.app.domain.ph.PhStatus
import com.genesyx.app.domain.streaks.StreakEngine
import com.genesyx.app.notifications.model.ReminderKind
import com.genesyx.app.ui.navigation.Screen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * A–Z Q&A for Learn / citations / pH copy / routing after the 0–13 parity batch.
 * These are the questions a reviewer or QA pass would ask, pinned so a later edit cannot
 * silently undo the answers.
 */
class QaParityTest {

    private val today = LocalDate.of(2026, 8, 17)

    @Test
    fun `A library is 32 articles and 20 of them are readable on 17 Aug`() {
        assertEquals(32, learnArticles.size)
        val live = LearnDrip.published(today)
        assertEquals(20, live.size)
        assertTrue(live.all { it.publishedAt == null })
        assertEquals(12, LearnDrip.weeklySeries.size)
    }

    @Test
    fun `B every article has a hero a body and a unique slug`() {
        assertEquals(learnArticles.size, learnArticles.map { it.slug }.toSet().size)
        learnArticles.forEach {
            assertNotNull("${it.slug} missing hero", it.heroImage)
            assertTrue(it.body.isNotEmpty())
        }
    }

    @Test
    fun `C how-to slugs and tab help are live today`() {
        AppGuide.entries.forEach { e ->
            assertEquals(e.slug, LearnNavigation.publishedSlug(e.slug, today))
        }
        AppGuide.tabSignposts.forEach { slug ->
            assertEquals(slug, LearnNavigation.publishedSlug(slug, today))
        }
    }

    @Test
    fun `D dated weeks are named but do not open before their Sunday`() {
        LearnDrip.weeklySeries.forEach { article ->
            val dayBefore = article.publishedAt!!.minusDays(1)
            assertNull(article.slug, LearnNavigation.publishedSlug(article.slug, dayBefore))
            assertEquals(article.slug, LearnNavigation.publishedSlug(article.slug, article.publishedAt))
        }
    }

    @Test
    fun `E shettles iOS slug aliases and stays gated until 8 Nov`() {
        assertEquals(
            articleBySlug("shettles-method-theory-vs-evidence"),
            articleBySlug("shettles-method"),
        )
        assertNull(LearnNavigation.publishedSlug("shettles-method", LocalDate.of(2026, 11, 7)))
        assertEquals(
            "shettles-method-theory-vs-evidence",
            LearnNavigation.publishedSlug("shettles-method", LocalDate.of(2026, 11, 8)),
        )
    }

    @Test
    fun `F new-article reminder never names a future week`() {
        assertEquals("genesyx://learn", LearnNavigation.newArticleDeepLink(today))
        assertEquals(
            "genesyx://learn/article/fertile-window",
            LearnNavigation.newArticleDeepLink(LocalDate.of(2026, 8, 23)),
        )
        LearnDrip.weeklySeries.forEach { article ->
            val link = LearnNavigation.newArticleDeepLink(article.publishedAt!!.minusDays(1))
            assertFalse(article.slug, link.contains(article.slug))
        }
    }

    @Test
    fun `G science and shettles website buttons are not wired to the homepage`() {
        assertFalse(AppLinks.isConfiguredWebPage(AppLinks.SCIENCE_URL))
        assertFalse(AppLinks.isConfiguredWebPage(AppLinks.SHETTLES_THEORY_URL))
    }

    @Test
    fun `H every mapped Learn slug has real citations`() {
        LearnSourceMap.bySlug.forEach { (slug, _) ->
            val cites = LearnSourceMap.citationsFor(slug)
            assertTrue("$slug has no citations", cites.isNotEmpty())
            cites.forEach { assertTrue(it.url, it.url.startsWith("https://")) }
        }
    }

    @Test
    fun `I medical sources catalogue is the iOS set of 18`() {
        assertEquals(18, MedicalSources.all.size)
    }

    @Test
    fun `J starter guide is twenty pages and does not guarantee pregnancy`() {
        assertEquals(20, FreeGuideContent.pages.size)
        val text = FreeGuideContent.pages.flatMap { page ->
            page.blocks.map { block ->
                when (block) {
                    is FreeGuideBlock.Paragraph -> block.text
                    is FreeGuideBlock.Subheading -> block.text
                    is FreeGuideBlock.Bullets -> block.items.joinToString(" ")
                }
            }
        }.joinToString(" ").lowercase()
        assertTrue(text.contains("should not replace"))
        assertTrue(text.contains("no diet or lifestyle approach can guarantee pregnancy"))
    }

    @Test
    fun `K shettles cta target exists and is drip-gated independently`() {
        val shettles = articleBySlug("shettles-method-theory-vs-evidence")!!
        assertEquals(CtaType.OPEN_ARTICLE, shettles.cta!!.type)
        assertEquals("timing-sex-when-ttc", shettles.cta!!.targetSlug)
        assertNotNull(articleBySlug("timing-sex-when-ttc"))
        assertEquals(ArticleCategory.TRACKING, shettles.category)
    }

    @Test
    fun `L pH bands in copy match the engine`() {
        assertEquals(3.8, PhStatus.HEALTHY_MIN, 0.0)
        assertEquals(4.5, PhStatus.HEALTHY_MAX, 0.0)
        assertTrue(PhCopy.WHY_BODY.contains("3.8") && PhCopy.WHY_BODY.contains("4.5"))
        assertTrue(PhCopy.SHETTLES_BODY.contains("not a proven method"))
    }

    @Test
    fun `M recipes are eight real meals with photos and food groups`() {
        assertEquals(8, recipeContent.size)
        recipeContent.forEach {
            assertNotNull(it.imageRes)
            assertTrue(it.ingredients.size >= 3)
            assertTrue(it.groups.isNotEmpty())
        }
    }

    @Test
    fun `N pH is on partner and admin stay off reminders are local`() {
        assertTrue(FeatureFlags.PH_TRACKING)
        assertTrue(FeatureFlags.PUSH_NOTIFICATIONS)
        assertFalse(FeatureFlags.PARTNER_INVITES)
        assertFalse(FeatureFlags.ADMIN_CLIENTS)
    }

    @Test
    fun `O hydration goal is user-set in millilitres`() {
        assertEquals(2400, StreakEngine.DEFAULT_GOAL_ML)
        assertEquals(1000, StreakEngine.GOAL_RANGE_ML.first)
        assertEquals(5000, StreakEngine.GOAL_RANGE_ML.last)
        assertEquals(4, StreakEngine.WEEK_COMPLETE_DAYS)
    }

    @Test
    fun `P vaginal pH input and two-band classification`() {
        assertEquals(3.8, PhStatus.MIN, 0.0)
        assertEquals(7.0, PhStatus.MAX, 0.0)
        assertEquals(0.1, PhStatus.STEP, 0.0)
        assertEquals(4.2, PhStatus.DEFAULT, 0.0)
        assertEquals(PhStatus.HEALTHY, PhStatus.classify(4.5))
        assertEquals(PhStatus.ELEVATED, PhStatus.classify(4.6))
        assertEquals(PhStatus.HEALTHY, PhStatus.classify(3.8))
    }

    @Test
    fun `Q Learn free-guide and medical-sources routes exist and hide the tab bar`() {
        assertEquals("learn/free-guide", Screen.FreeGuide.route)
        assertEquals("learn/medical-sources", Screen.MedicalSources.route)
        assertTrue(Screen.noBottomNavRoutes.contains(Screen.FreeGuide.route))
        assertTrue(Screen.noBottomNavRoutes.contains(Screen.MedicalSources.route))
        assertEquals(7, Screen.bottomTabs.size)
        assertTrue(Screen.bottomTabs.contains(Screen.Learn))
        assertTrue(Screen.bottomTabs.contains(Screen.PhDetail))
    }

    @Test
    fun `R a new-article reminder falls back to the Learn tab`() {
        assertEquals("genesyx://learn", ReminderKind.NEW_ARTICLE.deepLink)
        assertEquals("genesyx://log", ReminderKind.DAILY_LOG.deepLink)
        assertEquals("genesyx://tracker/ph", Screen.PhDetail.route.let { "genesyx://$it" }.replace("tracker/ph", "tracker/ph"))
        assertEquals("tracker/ph", Screen.PhDetail.route)
    }

    @Test
    fun `S citation lines omit an empty reviewed date`() {
        val withDate = Citation(title = "Vaginal discharge", publisher = "NHS", reviewed = "15 February 2024", url = "https://www.nhs.uk")
        val without = Citation(title = "Folate", publisher = "EFSA", reviewed = "", url = "https://www.efsa.europa.eu")
        assertEquals("NHS · Vaginal discharge · reviewed 15 February 2024", withDate.line)
        assertEquals("EFSA · Folate", without.line)
        assertFalse(without.line.contains("reviewed"))
    }

    @Test
    fun `T privacy and delete-account are real pages science is not`() {
        assertTrue(AppLinks.isConfiguredWebPage(AppLinks.PRIVACY_POLICY_URL))
        assertTrue(AppLinks.isConfiguredWebPage(AppLinks.DELETE_ACCOUNT_URL))
        assertFalse(AppLinks.isConfiguredWebPage(AppLinks.SCIENCE_URL))
        assertFalse(AppLinks.isConfiguredWebPage(AppLinks.SHETTLES_THEORY_URL))
        assertEquals("info@genesyx.co.uk", AppLinks.SUPPORT_EMAIL)
    }

    @Test
    fun `U pH copy never names a condition or a treatment`() {
        val banned = listOf(
            "bacterial vaginosis", "thrush", "candida", "yeast", "treat", "cure", "diagnos",
            "boy or girl", "alkaline diet",
        )
        val all = PhCopy.all().joinToString(" ").lowercase()
        banned.forEach { phrase ->
            assertFalse("pH copy contains banned phrase '$phrase'", all.contains(phrase))
        }
        assertTrue(PhCopy.DISCLAIMER.contains("isn't medical advice"))
        assertEquals("urine (legacy)", PhCopy.LEGACY_MARKER)
    }

    @Test
    fun `V every Learn hero drawable is unique`() {
        val heroes = learnArticles.map { it.heroImage }
        assertTrue(heroes.all { it != null })
        assertEquals(heroes.size, heroes.toSet().size)
    }

    @Test
    fun `W the 12-week plan is Sunday-gated from 23 Aug to 8 Nov 2026`() {
        val first = LearnDrip.weeklySeries.first()
        val last = LearnDrip.weeklySeries.last()
        assertEquals(LocalDate.of(2026, 8, 23), first.publishedAt)
        assertEquals(LocalDate.of(2026, 11, 8), last.publishedAt)
        assertEquals(12, LearnDrip.weeklySeries.size)
        assertEquals("fertile-window", first.slug)
        assertEquals("shettles-method-theory-vs-evidence", last.slug)
    }

    @Test
    fun `X article deep-link builder uses the canonical slug`() {
        assertEquals("learn/article/timing-sex-when-ttc", Screen.ArticleDetail.create("timing-sex-when-ttc"))
        assertEquals(
            "shettles-method-theory-vs-evidence",
            articleBySlug(AppLinks.SHETTLES_IOS_SLUG)!!.slug,
        )
        assertEquals(AppLinks.SHETTLES_ARTICLE_SLUG, articleBySlug(AppLinks.SHETTLES_IOS_SLUG)!!.slug)
    }

    @Test
    fun `Y medical sources are all https and uniquely id'd`() {
        assertEquals(18, MedicalSources.all.map { it.id }.toSet().size)
        MedicalSources.all.forEach {
            assertTrue(it.id, it.url.startsWith("https://"))
            assertTrue(it.title.isNotBlank())
            assertTrue(it.publisher.isNotBlank())
        }
    }

    @Test
    fun `Z starter guide pages have a heading and at least one block`() {
        FreeGuideContent.pages.forEach { page ->
            assertTrue(page.heading, page.heading.isNotBlank())
            assertTrue(page.heading, page.blocks.isNotEmpty())
        }
        val joined = FreeGuideContent.pages.joinToString(" ") { it.heading }.lowercase()
        assertFalse(joined.contains("pcos"))
        assertFalse(joined.contains("endometriosis"))
    }
}
