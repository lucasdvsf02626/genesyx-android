package com.genesyx.app.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecoveryLinkTest {

    @Test
    fun `redirect url matches the iOS constant so both platforms share one allow-list entry`() {
        assertEquals("genesyx://reset-password", RecoveryLink.REDIRECT_URL)
    }

    @Test
    fun `a link with both tokens carries a session`() {
        assertTrue(
            RecoveryLink.carriesSession(
                "genesyx://reset-password#access_token=abc&expires_in=3600&refresh_token=def&token_type=bearer&type=recovery",
            ),
        )
    }

    @Test
    fun `an expired or already-used link does not carry a session`() {
        // The exact shape Supabase sends back for an expired link — this fragment made
        // supabase-kt's own parser throw, which is why RecoveryLink checks first.
        assertFalse(
            RecoveryLink.carriesSession(
                "genesyx://reset-password#error=access_denied&error_code=otp_expired&error_description=Email+link+is+invalid+or+has+expired",
            ),
        )
    }

    @Test
    fun `a link with no fragment carries nothing`() {
        assertFalse(RecoveryLink.carriesSession("genesyx://reset-password"))
    }

    @Test
    fun `missing either token is not a session`() {
        assertFalse(RecoveryLink.carriesSession("genesyx://reset-password#access_token=abc"))
        assertFalse(RecoveryLink.carriesSession("genesyx://reset-password#refresh_token=def"))
        assertFalse(RecoveryLink.carriesSession("genesyx://reset-password#access_token=&refresh_token=def"))
    }
}
