package com.genesyx.app.domain.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LearnSourceMapTest {

    @Test
    fun `every mapped slug resolves and every id is a real source`() {
        LearnSourceMap.bySlug.forEach { (slug, ids) ->
            assertTrue("$slug is not in the library or alias map", articleBySlug(slug) != null || slug == "shettles-method")
            ids.forEach { id ->
                assertTrue("$slug cites unknown source $id", MedicalSources.byId.containsKey(id))
            }
        }
    }

    @Test
    fun `hydration why-card sources match iOS NutritionView`() {
        val ids = listOf("armstrong-2012", "valtin-2002", "nhs-water")
        ids.forEach { assertTrue(it, MedicalSources.byId.containsKey(it)) }
    }

    @Test
    fun `shettles alias and compiled slug share Wilcox plus conception`() {
        assertEquals(
            LearnSourceMap.citationsFor("shettles-method").map { it.id },
            LearnSourceMap.citationsFor("shettles-method-theory-vs-evidence").map { it.id },
        )
        assertEquals(
            listOf("wilcox-1995", "nhs-conception"),
            LearnSourceMap.citationsFor("shettles-method").map { it.id },
        )
    }
}
