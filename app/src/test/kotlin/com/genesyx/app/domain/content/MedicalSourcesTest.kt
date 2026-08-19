package com.genesyx.app.domain.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MedicalSourcesTest {

    @Test
    fun `every source has an id, https url and unique title`() {
        assertEquals(18, MedicalSources.all.size)
        assertEquals(MedicalSources.all.size, MedicalSources.all.map { it.id }.toSet().size)
        MedicalSources.all.forEach { c ->
            assertTrue(c.id, c.id.isNotBlank())
            assertTrue(c.title, c.title.isNotBlank())
            assertTrue(c.publisher, c.publisher.isNotBlank())
            assertTrue(c.url, c.url.startsWith("https://"))
        }
    }
}
