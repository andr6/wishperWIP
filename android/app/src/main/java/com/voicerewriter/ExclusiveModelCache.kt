package com.voicerewriter

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Owns one native model handle at a time.
 *
 * Load, inference and release share the same mutex. A context can never be
 * released by another warm-up or model switch while decode is using it.
 * The caller provides JNI-specific load/release functions, so this coordinator
 * can be exercised with lightweight fake handles in JVM concurrency tests.
 */
internal class ExclusiveModelCache<T : Any> {
    private val mutex = Mutex()
    private var id: String? = null
    private var handle: T? = null

    suspend fun <R> withModel(
        modelId: String,
        load: suspend () -> T,
        release: suspend (T) -> Unit,
        operation: suspend (T) -> R,
    ): R = mutex.withLock {
        require(modelId.isNotBlank()) { "Model ID must not be blank" }
        var current = handle
        if (current == null || id != modelId) {
            // Clear cache first: failed/cancelled release or load must never
            // expose an already released native pointer to a later caller.
            handle = null
            id = null
            if (current != null) release(current)
            current = load()
            handle = current
            id = modelId
        }
        operation(requireNotNull(current))
    }

    suspend fun warm(
        modelId: String,
        load: suspend () -> T,
        release: suspend (T) -> Unit,
    ) {
        withModel(modelId, load, release) { }
    }
}
