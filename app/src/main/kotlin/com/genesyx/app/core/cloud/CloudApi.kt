package com.genesyx.app.core.cloud

import com.genesyx.app.core.config.AppConfig
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Abstraction over a future Google Cloud backend (Cloud Functions / Cloud Run / API Gateway). Reads
 * its base URL from [AppConfig] (BuildConfig GENESYX_API_BASE_URL) so dev/staging/prod differ by
 * build config, not code. No transport is wired yet — this is the extension point.
 *
 * TODO(gcloud): add typed endpoints here (e.g. suspend fun callFunction(name, payload)) implemented
 * over Ktor/OkHttp with the app's auth token attached.
 */
interface CloudApi {
    val baseUrl: String
    val isConfigured: Boolean
}

@Singleton
class DefaultCloudApi @Inject constructor(private val config: AppConfig) : CloudApi {
    override val baseUrl: String get() = config.apiBaseUrl
    override val isConfigured: Boolean get() = config.hasCloudApi
}
