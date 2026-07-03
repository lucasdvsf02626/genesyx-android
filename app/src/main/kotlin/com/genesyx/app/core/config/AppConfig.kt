package com.genesyx.app.core.config

/** Deploy target. Drives logging verbosity and (later) which Supabase/GCloud endpoints are used. */
enum class Environment { DEV, STAGING, PROD }

/**
 * Single source of truth for environment-specific configuration. Built from [com.genesyx.app.BuildConfig]
 * fields (see app/build.gradle.kts) so dev/staging/prod differ by build config, never by hard-coded
 * constants scattered in code. This is the seam Supabase and Google Cloud wiring reads from.
 */
data class AppConfig(
    val environment: Environment,
    val supabaseUrl: String,
    val supabaseAnonKey: String,
    /** Optional Google Cloud / backend API base (Cloud Functions, Cloud Run). Empty until wired. */
    val apiBaseUrl: String,
    /** Google OAuth Web client ID for `signInWith(IDToken)`. Empty until Google sign-in is wired. */
    val googleWebClientId: String,
) {
    val hasSupabase: Boolean get() = supabaseUrl.isNotBlank() && supabaseAnonKey.isNotBlank()
    val hasCloudApi: Boolean get() = apiBaseUrl.isNotBlank()
    val hasGoogleAuth: Boolean get() = googleWebClientId.isNotBlank()
}
