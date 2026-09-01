package com.genesyx.app.auth

import androidx.annotation.StringRes
import com.genesyx.app.R
import com.genesyx.app.core.result.DataResult
import io.github.jan.supabase.auth.exception.AuthErrorCode
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
import java.io.IOException

/**
 * What actually went wrong with an auth call, mapped once in the service layer so no screen ever
 * has to render `t.message` (raw GoTrue/Postgrest text) or sniff strings. Modelled on the
 * `googleErrorText` precedent in AuthViewModel — but typed, because Supabase gives us real codes.
 */
enum class AuthErrorKind {
    /** 429 / over_*_rate_limit — includes the 2-emails-per-hour auth-email throttle. */
    RATE_LIMITED,

    /** The request never got an answer: no network, DNS, timeout. */
    OFFLINE,

    /** The provider examined the credentials and said no. Only this kind may blame the password. */
    INVALID_CREDENTIALS,

    /** Sign-in refused because the address hasn't been confirmed yet. */
    EMAIL_NOT_CONFIRMED,

    /** Anything else — surfaced as neutral copy, logged with the original throwable. */
    UNKNOWN,
}

/**
 * Wrapper thrown/returned by [SupabaseAuthService] so [DataResult.Error.throwable] carries the
 * mapped kind alongside the original cause (which keeps its place in the logs).
 */
class AuthError(val kind: AuthErrorKind, cause: Throwable) : Exception(cause.message, cause)

/** Classify a raw supabase-kt / transport failure. Pure; unit-tested in AuthErrorKindTest. */
fun authErrorKindOf(t: Throwable): AuthErrorKind = when (t) {
    is AuthError -> t.kind
    is AuthRestException -> when (t.errorCode) {
        AuthErrorCode.OverEmailSendRateLimit,
        AuthErrorCode.OverRequestRateLimit,
        AuthErrorCode.OverSmsSendRateLimit,
        -> AuthErrorKind.RATE_LIMITED
        AuthErrorCode.InvalidCredentials -> AuthErrorKind.INVALID_CREDENTIALS
        AuthErrorCode.EmailNotConfirmed -> AuthErrorKind.EMAIL_NOT_CONFIRMED
        else -> if (t.statusCode == 429) AuthErrorKind.RATE_LIMITED else AuthErrorKind.UNKNOWN
    }
    is RestException -> if (t.statusCode == 429) AuthErrorKind.RATE_LIMITED else AuthErrorKind.UNKNOWN
    is HttpRequestException, is IOException -> AuthErrorKind.OFFLINE
    else -> AuthErrorKind.UNKNOWN
}

/** The kind carried by a failed auth result; anything unmapped is [AuthErrorKind.UNKNOWN]. */
fun DataResult.Error.authErrorKind(): AuthErrorKind =
    throwable?.let { authErrorKindOf(it) } ?: AuthErrorKind.UNKNOWN

/** Default copy per kind. Context-specific screens may override (e.g. "current password"). */
@StringRes
fun AuthErrorKind.messageRes(): Int = when (this) {
    AuthErrorKind.RATE_LIMITED -> R.string.auth_error_rate_limited
    AuthErrorKind.OFFLINE -> R.string.auth_error_offline
    AuthErrorKind.INVALID_CREDENTIALS -> R.string.auth_error_invalid_credentials
    AuthErrorKind.EMAIL_NOT_CONFIRMED -> R.string.auth_error_email_not_confirmed
    AuthErrorKind.UNKNOWN -> R.string.auth_error_unknown
}
