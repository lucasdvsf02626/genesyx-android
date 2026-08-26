package com.genesyx.app.data

import com.genesyx.app.domain.model.Supplement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The plan sheet's bell stores reminders under ids that reconcile() must not mistake for deleted entries. */
class SupplementReminderPlanIdTest {

    @Test
    fun `a plan reminder id round-trips to the supplement's display name`() {
        Supplement.defaultPlan.forEach { s ->
            val id = SupplementReminderRepository.planReminderId(s)
            assertEquals(s.displayName, SupplementReminderRepository.planReminderName(id))
        }
    }

    @Test
    fun `a user-supplement id is not a plan reminder`() {
        assertNull(SupplementReminderRepository.planReminderName("3f0c8a5e-user-supplement"))
        assertNull(SupplementReminderRepository.planReminderName("plan:not-a-supplement"))
        assertNull(SupplementReminderRepository.planReminderName(""))
    }
}
