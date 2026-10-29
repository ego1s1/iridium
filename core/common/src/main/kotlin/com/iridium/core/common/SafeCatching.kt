package com.iridium.core.common

import kotlinx.coroutines.CancellationException

/**
 * [runCatching] that never swallows cancellation. Kotlin's `runCatching`
 * catches [CancellationException] too, which breaks structured concurrency:
 * a cancelled coroutine keeps running and the caller's cancellation is
 * lost. Use this in any suspend context.
 */
@Suppress("TooGenericExceptionCaught")
inline fun <T> safeCatching(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    Result.failure(e)
}
