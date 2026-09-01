package com.genesyx.app.auth

import io.github.jan.supabase.exceptions.RestException
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * The deletion-retry recognition (audit P1 #9): after a dropped response, the retry finds the
 * account gone — that must read as success, or the local wipe stays unreachable forever.
 */
class AccountAlreadyGoneTest {

    @Test
    fun `the RPC's own no-authenticated-user error means the account is gone`() {
        assertTrue(
            accountAlreadyGone(RestException("P0001", "no authenticated user", 400, "no authenticated user")),
        )
    }

    @Test
    fun `errcode 28000 means the account is gone`() {
        assertTrue(accountAlreadyGone(RestException("28000", null, 400, "invalid_authorization_specification")))
    }

    @Test
    fun `a 401 on the deletion retry means the refreshed token died with the account`() {
        assertTrue(accountAlreadyGone(RestException("PGRST301", "JWT expired", 401, "Unauthorized")))
    }

    @Test
    fun `a network failure is NOT the account being gone`() {
        // Treating a timeout as success would wipe local data while the server row still exists.
        assertFalse(accountAlreadyGone(IOException("timeout")))
    }

    @Test
    fun `an ordinary server error is NOT the account being gone`() {
        assertFalse(accountAlreadyGone(RestException("server_error", "boom", 500, "Internal Server Error")))
    }
}
