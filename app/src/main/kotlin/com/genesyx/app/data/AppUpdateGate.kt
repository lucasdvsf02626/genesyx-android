package com.genesyx.app.data

import com.genesyx.app.core.di.ApplicationScope
import com.genesyx.app.core.log.Logger
import com.genesyx.app.core.result.DataResult
import com.genesyx.app.data.remote.AppConfigRemoteDataSource
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Minimum-version gate. At startup the app anonymously reads `app_config.min_supported_build` and
 * compares it against the running `BuildConfig.VERSION_CODE`; a build below the minimum is met with
 * a non-dismissible update screen (see `UpdateRequiredScreen`).
 *
 * FAIL-OPEN BY CONTRACT: a missing table, a missing row, a network error and a malformed value all
 * allow the launch. The gate only ever blocks on a confirmed, well-formed answer above this build —
 * a deployment slip or an offline phone must never lock users out.
 */
@Singleton
class AppUpdateGate @Inject constructor(
    private val remote: AppConfigRemoteDataSource,
    private val logger: Logger,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val _updateRequired = MutableStateFlow(false)
    val updateRequired: StateFlow<Boolean> = _updateRequired.asStateFlow()

    private val attempted = AtomicBoolean(false)

    /** One check per process, on the app scope so the splash/activity lifecycle can't cancel it. */
    fun check(currentVersionCode: Int) {
        if (!attempted.compareAndSet(false, true)) return
        scope.launch {
            val required = isUpdateRequired(currentVersionCode, remote.getValue(KEY_MIN_SUPPORTED_BUILD))
            if (required) {
                logger.w("AppUpdate", "build $currentVersionCode is below min_supported_build — update required")
            }
            _updateRequired.value = required
        }
    }

    companion object {
        const val KEY_MIN_SUPPORTED_BUILD = "min_supported_build"
    }
}

/**
 * The gate's whole decision, pure and top-level so it is unit-testable without a remote (the
 * `accountAlreadyGone` precedent). Anything but a well-formed integer strictly above
 * [currentVersionCode] fails OPEN.
 */
internal fun isUpdateRequired(currentVersionCode: Int, result: DataResult<String?>): Boolean {
    val raw = (result as? DataResult.Success)?.data ?: return false
    val minimum = raw.trim().toIntOrNull() ?: return false
    return currentVersionCode < minimum
}
