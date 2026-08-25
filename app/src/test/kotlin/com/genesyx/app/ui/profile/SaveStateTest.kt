package com.genesyx.app.ui.profile

import com.genesyx.app.core.result.SaveOutcome
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The copy each outcome turns into. These are guards, not decoration: the point of splitting
 * refused from failed is lost the moment both render the same sentence, and the failure copy is
 * the only thing telling her the edit wasn't thrown away.
 */
class SaveStateTest {

    @Test
    fun `a save that landed closes the dialog and shows no error`() {
        val state = SaveOutcome.Saved.toSaveState()

        assertTrue(state.saved)
        assertNull(state.error)
        assertFalse(state.saving)
    }

    @Test
    fun `a refusal keeps the dialog open and names the control that fixes it`() {
        val state = SaveOutcome.Refused.toSaveState()

        assertFalse(state.saved)
        assertTrue(state.error!!.contains("Health data consent"))
    }

    @Test
    fun `a failure reads differently from a refusal`() {
        val refused = SaveOutcome.Refused.toSaveState().error
        val failed = SaveOutcome.Failed("boom").toSaveState().error

        assertNotEquals(refused, failed)
    }

    /**
     * These repositories write to Room before they push, so the edit survived. Copy that reads as
     * "your change is gone" would send her to redo work that is already saved.
     */
    @Test
    fun `a failure says the edit was kept on the device`() {
        val failed = SaveOutcome.Failed(null).toSaveState()

        assertFalse(failed.saved)
        assertTrue(failed.error!!.contains("Saved on this device"))
    }

    /** The underlying throwable message is for logs. Putting it in front of her explains nothing. */
    @Test
    fun `the raw server message is never shown`() {
        val state = SaveOutcome.Failed("PGRST301: JWT expired").toSaveState()

        assertFalse(state.error!!.contains("PGRST301"))
    }
}
