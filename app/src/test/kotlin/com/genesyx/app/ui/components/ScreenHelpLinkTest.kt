package com.genesyx.app.ui.components

import com.genesyx.app.domain.content.articleBySlug
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The tab help links are the app's own manual, reached from the screen they describe. Two ways they
 * can silently break, both caught here:
 *
 *  1. A slug is renamed in [com.genesyx.app.domain.content.learnArticles] and the link dead-ends on
 *     the "That article isn't available." screen.
 *  2. A target is given a `publishedAt`, which hides it behind the weekly drip — the link would then
 *     dead-end for every user until that date, which is exactly the failure the drip gate is
 *     designed to cause and exactly what a permanent help link must never hit.
 */
class ScreenHelpLinkTest {

    @Test
    fun `every help link points at an article that exists`() {
        HelpLinks.all.forEach { (text, slug) ->
            assertNotNull("Help link \"$text\" points at unknown slug \"$slug\"", articleBySlug(slug))
        }
    }

    @Test
    fun `every help link target is always available, never behind the drip`() {
        HelpLinks.all.forEach { (text, slug) ->
            val article = articleBySlug(slug)!!
            assertNull(
                "Help link \"$text\" targets \"$slug\", which is dripped to ${article.publishedAt}. " +
                    "A permanent help link must point at an always-available article.",
                article.publishedAt,
            )
        }
    }

    @Test
    fun `all five tabs have a help link`() {
        assertEquals(5, HelpLinks.all.size)
        assertEquals(5, HelpLinks.all.map { it.second }.distinct().size)
    }
}
