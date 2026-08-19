package com.genesyx.app.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLinksTest {

    @Test
    fun `the site root is not a configured science or shettles page`() {
        assertFalse(AppLinks.isConfiguredWebPage(AppLinks.SITE_URL))
        assertFalse(AppLinks.isConfiguredWebPage(AppLinks.SCIENCE_URL))
        assertFalse(AppLinks.isConfiguredWebPage(AppLinks.SHETTLES_THEORY_URL))
        assertFalse(AppLinks.isConfiguredWebPage(""))
        assertTrue(AppLinks.isConfiguredWebPage("https://genesyx.co.uk/pages/science"))
    }
}
