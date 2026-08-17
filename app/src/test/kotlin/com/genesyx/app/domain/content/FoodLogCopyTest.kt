package com.genesyx.app.domain.content

import com.genesyx.app.domain.model.Phase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FoodLogCopyTest {

    @Test
    fun `six groups with shared raw values and distinct copy`() {
        assertEquals(6, FoodGroup.entries.size)
        assertEquals(
            setOf("vegetables", "fruit", "starchyCarbs", "protein", "dairy", "oilsAndFats"),
            FoodGroup.entries.map { it.raw }.toSet(),
        )
        assertEquals(FoodGroup.entries.size, FoodGroup.entries.map { it.label }.toSet().size)
        assertEquals(FoodGroup.entries.size, FoodGroup.entries.map { it.examples }.toSet().size)
    }

    @Test
    fun `unknown tokens do not count toward the six`() {
        assertEquals(1, FoodGroup.knownCount(setOf("vegetables", "seaweed")))
        assertEquals(0, FoodGroup.knownCount(emptySet()))
    }

    @Test
    fun `empty day summary is not a zero score`() {
        val total = FoodGroup.entries.size
        assertFalse(FoodLogCopy.summary(0, total).contains("0"))
        assertTrue(FoodLogCopy.summary(1, total).contains("1 of $total"))
        assertTrue(FoodLogCopy.summary(total, total).contains("$total of $total"))
    }

    @Test
    fun `footnote says the log is not a scoreboard`() {
        assertTrue(FoodLogCopy.footnote.contains("not a target"))
        assertTrue(FoodLogCopy.footnote.contains("blank day costs you nothing"))
    }

    @Test
    fun `copy makes no health claim`() {
        val banned = listOf(
            "boost", "improve", "good for", "supports fertility", "conceive",
            "scoreboard", "points", "complete your", "you should eat",
        )
        FoodLogCopy.allStrings.forEach { text ->
            val lower = text.lowercase()
            banned.forEach { word ->
                assertFalse("\"$word\" in food-log copy: $text", lower.contains(word))
            }
        }
    }

    @Test
    fun `every phase names food groups and none twice`() {
        assertEquals(Phase.entries.toSet(), nutritionPhaseFoodGroups.keys)
        nutritionPhaseFoodGroups.forEach { (phase, groups) ->
            assertTrue("$phase names none", groups.isNotEmpty())
            assertEquals("$phase names a group twice", groups.toSet().size, groups.size)
        }
    }

    @Test
    fun `phase line is a statement about the screen`() {
        assertNull(FoodLogCopy.phaseLine(emptyList()))
        val line = FoodLogCopy.phaseLine(nutritionPhaseFoodGroups.getValue(Phase.PERIOD))
        assertTrue(line!!.startsWith("Your focus foods this phase lean on"))
    }

    @Test
    fun `sentence list is british`() {
        assertEquals("", FoodLogCopy.sentenceList(emptyList()))
        assertEquals("fruit", FoodLogCopy.sentenceList(listOf("fruit")))
        assertEquals("fruit and protein", FoodLogCopy.sentenceList(listOf("fruit", "protein")))
        assertEquals("fruit, protein and dairy", FoodLogCopy.sentenceList(listOf("fruit", "protein", "dairy")))
    }
}
