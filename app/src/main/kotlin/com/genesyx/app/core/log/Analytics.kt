package com.genesyx.app.core.log

import javax.inject.Inject
import javax.inject.Singleton

/** Product analytics/telemetry hook. No-op sink today; swap the binding for GA4/Firebase/Cloud later. */
interface Analytics {
    fun track(event: String, params: Map<String, Any?> = emptyMap())
}

@Singleton
class NoopAnalytics @Inject constructor(private val logger: Logger) : Analytics {
    override fun track(event: String, params: Map<String, Any?>) {
        logger.d("Analytics", "$event $params")
        // TODO(gcloud): forward to Firebase Analytics / Google Analytics 4 / Cloud logging.
    }
}
