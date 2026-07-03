package com.genesyx.app.core.result

/**
 * Uniform result wrapper for data-layer operations (local + remote). Repositories that perform
 * fallible work (network, DB) return this instead of throwing, so ViewModels can render
 * success / error / loading states without try/catch scattered through the UI.
 */
sealed interface DataResult<out T> {
    data class Success<T>(val data: T) : DataResult<T>
    data class Error(val throwable: Throwable, val message: String? = null) : DataResult<Nothing>
    data object Loading : DataResult<Nothing>
}

/** Run [block], mapping success/throwable into [DataResult]. */
inline fun <T> runCatchingResult(block: () -> T): DataResult<T> =
    try {
        DataResult.Success(block())
    } catch (t: Throwable) {
        DataResult.Error(t, t.message)
    }

inline fun <T, R> DataResult<T>.map(transform: (T) -> R): DataResult<R> = when (this) {
    is DataResult.Success -> DataResult.Success(transform(data))
    is DataResult.Error -> this
    DataResult.Loading -> DataResult.Loading
}

fun <T> DataResult<T>.getOrNull(): T? = (this as? DataResult.Success)?.data
