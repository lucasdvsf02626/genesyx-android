package com.genesyx.app.content

import android.graphics.BitmapFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.genesyx.app.domain.content.learnArticles
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Proves every Learn article now has a hero and that each `heroImage` drawable actually decodes to a
 * real 16:9 bitmap at the shipping spec (1080×602). Catches a missing/corrupt/wrong-size asset that a
 * plain compile would not — the resource id resolving is not the same as the file decoding.
 */
@RunWith(AndroidJUnit4::class)
class LearnHeroImagesTest {

    private val context get() = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun everyArticleHasAHero() {
        val missing = learnArticles.filter { it.heroImage == null }.map { it.slug }
        assertTrue("articles still without a hero image: $missing", missing.isEmpty())
    }

    @Test
    fun everyHeroDecodesAt1080x602() {
        // De-dupe: a few heroes are intentionally shared between sister articles.
        val heroIds = learnArticles.mapNotNull { it.heroImage }.toSet()
        assertTrue("expected hero drawables", heroIds.isNotEmpty())
        heroIds.forEach { resId ->
            val name = context.resources.getResourceEntryName(resId)
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeResource(context.resources, resId, opts)
            assertTrue("$name failed to decode", opts.outWidth > 0 && opts.outHeight > 0)
            assertEquals("$name width", 1080, opts.outWidth)
            assertEquals("$name height", 602, opts.outHeight)
        }
    }
}
