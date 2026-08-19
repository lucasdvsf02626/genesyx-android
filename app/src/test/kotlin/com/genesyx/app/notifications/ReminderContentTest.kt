package com.genesyx.app.notifications

import com.genesyx.app.notifications.model.ReminderKind
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The intimacy nudge is the only notification that can land on a lock screen someone else is
 * looking at, so its words are a privacy control, not decoration. This pins them: the copy may not
 * name the subject, and it may not drift into the fertility claim that [ReminderKind.FERTILE_WINDOW]
 * already carefully avoids making.
 */
class ReminderContentTest {

    @Test
    fun `the intimacy reminder names nothing a bystander could read`() {
        val copy = ReminderContent.of(ReminderKind.INTIMACY)
        val text = (copy.title + " " + copy.body).lowercase()
        listOf("sex", "intima", "fertile", "ovulat", "conceiv", "partner", "period", "cycle")
            .forEach { word ->
                assertFalse("Intimacy notification copy leaks '$word': \"$text\"", text.contains(word))
            }
    }

    @Test
    fun `every kind has non-empty copy`() {
        ReminderKind.entries.forEach { kind ->
            val copy = ReminderContent.of(kind)
            assertTrue("$kind has a blank title", copy.title.isNotBlank())
            assertTrue("$kind has a blank body", copy.body.isNotBlank())
        }
    }
}
