package com.genesyx.app.ui.screens

import org.junit.Assert.assertTrue
import org.junit.Test

/** The Google sign-in error copy must name the real cause — a config problem must not read as a
 *  network blip, which is exactly what sent us chasing the wrong thing. */
class AuthViewModelGoogleErrorTest {

    private fun text(config: Boolean = false, noCred: Boolean = false, msg: String? = null) =
        AuthViewModel.googleErrorText(isProviderConfig = config, isNoCredential = noCred, rawMessage = msg)

    @Test
    fun `a developer-config code 10 reads as an unregistered build, not a network error`() {
        val m = text(msg = "During begin sign in, failure response from one tap: 10: Developer console is not set up correctly.")
        assertTrue(m.contains("isn't registered for Google sign-in"))
        assertTrue("must not blame the network", !m.contains("connection"))
    }

    @Test
    fun `a provider-configuration failure also reads as a config problem`() {
        assertTrue(text(config = true).contains("isn't registered for Google sign-in"))
    }

    @Test
    fun `no credential reads as no account on the device`() {
        val m = text(noCred = true, msg = "No credentials available")
        assertTrue(m.contains("No Google account is available"))
    }

    @Test
    fun `a genuine network failure reads as a connection problem`() {
        val m = text(msg = "7: NETWORK_ERROR: Unable to resolve host")
        assertTrue(m.contains("check your connection"))
    }

    @Test
    fun `an unknown failure falls back to a plain retry message`() {
        val m = text(msg = "something odd happened")
        assertTrue(m.contains("couldn't complete"))
        assertTrue(m.contains("email and password"))
    }

    @Test
    fun `dev-config takes priority even if the message also mentions no credential`() {
        // The SHA-1 case frequently surfaces AS a no-credential result; the config signal wins.
        val m = text(noCred = true, msg = "10: caller has not been allowed")
        assertTrue(m.contains("isn't registered for Google sign-in"))
    }
}
