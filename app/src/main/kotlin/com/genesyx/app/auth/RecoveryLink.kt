package com.genesyx.app.auth

/**
 * The password-recovery deep link: `genesyx://reset-password`.
 *
 * Mirrors iOS `DeepLink.passwordRecoveryURL` so both platforms share one entry on the Supabase
 * Auth "Redirect URLs" allow-list. If that allow-list entry is missing, Supabase silently drops
 * `redirect_to` and the email falls back to the Site URL — which is the dead end this link exists
 * to fix.
 *
 * Pure Kotlin (no android.net.Uri) so the fragment checks are unit-testable on the JVM.
 */
object RecoveryLink {
    const val SCHEME = "genesyx"
    const val HOST = "reset-password"
    const val REDIRECT_URL = "$SCHEME://$HOST"

    /**
     * True when [url] carries an importable recovery session — both tokens present and no error.
     *
     * Checked BEFORE handing the URL to supabase-kt: `parseSessionFromUrl` throws on an error
     * fragment (`#error=access_denied&error_code=otp_expired&…` — an expired or already-used
     * link), and an unhandled throw here would crash the activity instead of showing the
     * "request a new link" path.
     */
    fun carriesSession(url: String): Boolean {
        val params = fragmentParams(url)
        return "error" !in params && "error_code" !in params &&
            !params["access_token"].isNullOrEmpty() && !params["refresh_token"].isNullOrEmpty()
    }

    private fun fragmentParams(url: String): Map<String, String> {
        val fragment = url.substringAfter('#', missingDelimiterValue = "")
        if (fragment.isEmpty()) return emptyMap()
        return fragment.split("&").mapNotNull { part ->
            val key = part.substringBefore('=')
            if (key.isEmpty()) null else key to part.substringAfter('=', missingDelimiterValue = "")
        }.toMap()
    }
}
