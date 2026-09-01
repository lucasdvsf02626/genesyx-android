package com.genesyx.app.auth

import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
import io.ktor.client.request.HttpRequestBuilder
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException
import java.net.UnknownHostException

/**
 * The service-layer error mapping (audit P1 #4/#5): a 429 must never look like offline, and only
 * an answered credential rejection may ever be blamed on the typed password.
 */
class AuthErrorKindTest {

    @Test
    fun `the email throttle maps to RATE_LIMITED`() {
        // The exact code GoTrue sends when the 2-emails-per-hour budget is spent.
        assertEquals(
            AuthErrorKind.RATE_LIMITED,
            authErrorKindOf(AuthRestException("over_email_send_rate_limit", "rate limit exceeded", 429)),
        )
    }

    @Test
    fun `request-rate throttling maps to RATE_LIMITED too`() {
        assertEquals(
            AuthErrorKind.RATE_LIMITED,
            authErrorKindOf(AuthRestException("over_request_rate_limit", "rate limit exceeded", 429)),
        )
    }

    @Test
    fun `an unnamed 429 still maps to RATE_LIMITED`() {
        assertEquals(
            AuthErrorKind.RATE_LIMITED,
            authErrorKindOf(RestException("unknown", null, 429, "Too Many Requests")),
        )
    }

    @Test
    fun `invalid credentials map to INVALID_CREDENTIALS`() {
        assertEquals(
            AuthErrorKind.INVALID_CREDENTIALS,
            authErrorKindOf(AuthRestException("invalid_credentials", "Invalid login credentials", 400)),
        )
    }

    @Test
    fun `unconfirmed email maps to EMAIL_NOT_CONFIRMED`() {
        assertEquals(
            AuthErrorKind.EMAIL_NOT_CONFIRMED,
            authErrorKindOf(AuthRestException("email_not_confirmed", "Email not confirmed", 400)),
        )
    }

    @Test
    fun `network failures map to OFFLINE`() {
        assertEquals(AuthErrorKind.OFFLINE, authErrorKindOf(IOException("timeout")))
        assertEquals(AuthErrorKind.OFFLINE, authErrorKindOf(UnknownHostException("no dns")))
        assertEquals(
            AuthErrorKind.OFFLINE,
            authErrorKindOf(HttpRequestException("connect failed", HttpRequestBuilder())),
        )
    }

    @Test
    fun `anything else maps to UNKNOWN, never to a credential claim`() {
        assertEquals(AuthErrorKind.UNKNOWN, authErrorKindOf(IllegalStateException("weird")))
        assertEquals(AuthErrorKind.UNKNOWN, authErrorKindOf(RestException("server_error", null, 500, "boom")))
    }

    @Test
    fun `a pre-wrapped AuthError keeps its kind`() {
        val wrapped = AuthError(AuthErrorKind.EMAIL_NOT_CONFIRMED, IllegalStateException("no session"))
        assertEquals(AuthErrorKind.EMAIL_NOT_CONFIRMED, authErrorKindOf(wrapped))
    }
}
