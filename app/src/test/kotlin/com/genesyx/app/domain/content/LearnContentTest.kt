package com.genesyx.app.domain.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LearnContentTest {

    @Test
    fun `every article has copy`() {
        assertTrue(learnArticles.isNotEmpty())
        learnArticles.forEach {
            assertTrue(it.title.isNotBlank())
            assertTrue(it.excerpt.isNotBlank())
            assertTrue(it.readingTime.isNotBlank())
            assertTrue(it.body.isNotEmpty())
        }
    }

    /** Slugs are route keys — a duplicate silently shadows an article. */
    @Test
    fun `slugs are unique`() {
        assertEquals(learnArticles.size, learnArticles.map { it.slug }.toSet().size)
    }

    /** The Nutrition tiles used to open one shared paragraph. Each article must be its own read. */
    @Test
    fun `articles have distinct titles and bodies`() {
        assertEquals(learnArticles.size, learnArticles.map { it.title }.toSet().size)
        assertEquals(learnArticles.size, learnArticles.map { it.body }.toSet().size)
    }

    @Test
    fun `articleBySlug resolves every article and rejects an unknown slug`() {
        learnArticles.forEach { assertEquals(it, articleBySlug(it.slug)) }
        assertNull(articleBySlug("not-a-real-slug"))
    }

    @Test
    fun `exactly one article is featured`() {
        assertEquals(1, learnArticles.count { it.featured })
    }

    @Test
    fun `ten launch articles, each tagged`() {
        assertEquals(10, learnArticles.size)
        learnArticles.forEach { assertTrue("${it.slug} has no tags", it.tags.isNotEmpty()) }
    }

    /** A related link to a missing id silently vanishes from the UI — catch it here instead. */
    @Test
    fun `related article ids resolve and never self-reference`() {
        learnArticles.forEach { article ->
            assertEquals(
                "${article.slug} has an unresolvable related id",
                article.relatedArticleIds.size,
                relatedArticles(article).size,
            )
            assertTrue(
                "${article.slug} lists itself as related",
                article.id !in article.relatedArticleIds,
            )
        }
    }

    @Test
    fun `search matches title, excerpt and tags, and ignores case`() {
        assertTrue(searchArticles("hydration").any { it.slug == "hydration-basics" })
        assertTrue(searchArticles("HYDRATION").any { it.slug == "hydration-basics" })
        // "memory" appears only in a2's tags, not its title or excerpt.
        assertTrue(searchArticles("memory").any { it.slug == "why-logging-beats-remembering" })
        assertTrue(searchArticles("zzzznothing").isEmpty())
        assertTrue(searchArticles("   ").isEmpty())
    }

    /** An OPEN_ARTICLE CTA pointing at a missing slug is a dead end the user can reach. */
    @Test
    fun `article CTAs resolve`() {
        learnArticles.mapNotNull { it.cta }
            .filter { it.type == CtaType.OPEN_ARTICLE }
            .forEach { assertNotNull(articleBySlug(requireNotNull(it.targetSlug))) }
    }

    /**
     * Health content must carry the disclaimer. See docs/V1_1_NOTIFICATIONS_AND_LEARN.md §8.0 — a
     * pH/fertility overclaim shipped once already and was caught by hand the night before release.
     */
    @Test
    fun `nutrition and wellness articles require the disclaimer`() {
        learnArticles
            .filter { it.category == ArticleCategory.NUTRITION || it.category == ArticleCategory.WELLNESS }
            .forEach { assertTrue("${it.slug} must set disclaimerRequired", it.disclaimerRequired) }
    }

    /**
     * Category alone can't express this: `what-insights-mean` is INSIGHTS and correctly carries no
     * disclaimer, while `reading-your-trends` and `using-what-you-learn` are INSIGHTS and do. Pin the
     * exact set so a flag cannot be dropped silently. Adding health content? Add its slug here too.
     */
    @Test
    fun `the set of articles carrying the disclaimer is exactly this`() {
        assertEquals(
            setOf(
                "hydration-basics",
                "eating-with-your-cycle",
                "gentle-guide-supplements",
                "reading-your-trends",
                "small-habits-that-hold",
                "using-what-you-learn",
            ),
            learnArticles.filter { it.disclaimerRequired }.map { it.slug }.toSet(),
        )
    }

    /** ArticleCta.init enforces this; the test documents it and fails loudly if that guard is removed. */
    @Test
    fun `an OPEN_ARTICLE cta always carries a resolvable targetSlug`() {
        learnArticles.mapNotNull { it.cta }
            .filter { it.type == CtaType.OPEN_ARTICLE }
            .forEach { assertNotNull(articleBySlug(requireNotNull(it.targetSlug))) }

        val bad = runCatching { ArticleCta(CtaType.OPEN_ARTICLE, "Read more", targetSlug = null) }
        assertTrue("ArticleCta must reject OPEN_ARTICLE with a null targetSlug", bad.isFailure)
    }

    /**
     * Every user-visible string on an article, lowercased and space-joined. Anything a reader can see
     * must be scanned by [no banned health claims appear in any article] — tags are searchable and CTA
     * labels are rendered on a button, so both count.
     */
    private fun Article.searchableText(): String = (
        listOf(title, excerpt) +
            tags +
            listOfNotNull(cta?.label) +
            body.map { block ->
                when (block) {
                    is ArticleBlock.Heading -> block.text
                    is ArticleBlock.Paragraph -> block.text
                    is ArticleBlock.Callout -> block.text
                    is ArticleBlock.BulletList -> block.items.joinToString(" ")
                }
            }
        ).joinToString(" ").lowercase()

    /** Release blocker, not a lint warning. Sex-selection and pH-"balancing" framing must never return. */
    @Test
    fun `no banned health claims appear in any article`() {
        val banned = listOf(
            "boy or girl", "sex-selection", "sway", "alkaline diet", "alkaline water",
            "douch", "optimize your ph", "balance your ph", "conceiving a boy", "conceiving a girl",
        )
        learnArticles.forEach { article ->
            val text = article.searchableText()
            banned.forEach { phrase ->
                assertTrue("${article.slug} contains banned phrase: $phrase", !text.contains(phrase))
            }
        }
    }

    /** Guards the guard: if a field stops being scanned, this fails before the phrase check can pass. */
    @Test
    fun `banned-phrase scan covers title, excerpt, tags, cta label and body`() {
        val a = learnArticles.first { it.slug == "why-logging-beats-remembering" }
        val text = a.searchableText()
        assertTrue("title not scanned", text.contains("why logging beats remembering"))
        assertTrue("excerpt not scanned", text.contains("memory rewrites the past"))
        assertTrue("tags not scanned", text.contains("memory"))
        assertTrue("cta label not scanned", text.contains("log how you feel today"))
        assertTrue("body paragraph not scanned", text.contains("memory is reconstructive"))
        assertTrue("body bullet not scanned", text.contains("\"nothing to report\" is data"))
        assertTrue("body heading not scanned", text.contains("what a log actually does"))
    }
}
