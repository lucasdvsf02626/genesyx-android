package com.genesyx.app.core.log

import android.util.Log
import com.genesyx.app.core.config.AppConfig
import com.genesyx.app.core.config.Environment

/**
 * Structured logging seam. Today it writes to Logcat; the same interface is the extension point
 * for a Google Cloud Logging / Crashlytics / analytics sink later — swap the Hilt binding in
 * core.di.CoreModule without touching call sites.
 */
interface Logger {
    fun d(tag: String, message: String)
    fun i(tag: String, message: String)
    fun w(tag: String, message: String, throwable: Throwable? = null)
    fun e(tag: String, message: String, throwable: Throwable? = null)
}

/** Logcat-backed logger. Debug is suppressed outside DEV to keep prod logs clean. */
class AndroidLogger(private val config: AppConfig) : Logger {
    override fun d(tag: String, message: String) {
        if (config.environment == Environment.DEV) Log.d(tag, message)
    }

    override fun i(tag: String, message: String) {
        Log.i(tag, message)
    }

    override fun w(tag: String, message: String, throwable: Throwable?) {
        Log.w(tag, message, throwable)
    }

    override fun e(tag: String, message: String, throwable: Throwable?) {
        Log.e(tag, message, throwable)
        // TODO(cloud): forward errors to Crashlytics / Google Cloud Error Reporting here.
    }
}
