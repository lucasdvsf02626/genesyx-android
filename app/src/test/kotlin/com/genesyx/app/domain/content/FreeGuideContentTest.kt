package com.genesyx.app.domain.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FreeGuideContentTest {

    @Test
    fun `the starter guide has twenty pages of real copy`() {
        assertEquals("7-Day Fertility Nutrition Starter Guide", FreeGuideContent.title)
        assertEquals(20, FreeGuideContent.pages.size)
        assertEquals((1..20).toList(), FreeGuideContent.pages.map { it.number })
        FreeGuideContent.pages.forEach { page ->
            assertTrue(page.heading, page.heading.isNotBlank())
            assertTrue(page.heading, page.blocks.isNotEmpty())
        }
    }
}
