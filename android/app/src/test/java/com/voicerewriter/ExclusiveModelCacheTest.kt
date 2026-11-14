package com.voicerewriter

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test

class ExclusiveModelCacheTest {
    private data class Handle(val name: String)

    @Test fun warmAndDecodeReuseOneNativeModel() = runBlocking {
        val cache = ExclusiveModelCache<Handle>()
        val loaded = mutableListOf<String>()
        val released = mutableListOf<String>()
        suspend fun loadA() = Handle("a").also { loaded.add(it.name) }
        suspend fun release(handle: Handle) { released.add(handle.name) }

        cache.warm("a", ::loadA, ::release)
        assertEquals("a", cache.withModel("a", ::loadA, ::release) { it.name })
        assertEquals(listOf("a"), loaded)
        assertTrue(released.isEmpty())
    }

    @Test fun modelSwitchWaitsForInFlightDecodeBeforeReleasingHandle() = runBlocking {
        withTimeout(5_000) {
            val cache = ExclusiveModelCache<Handle>()
            val entered = CompletableDeferred<Unit>()
            val finishDecode = CompletableDeferred<Unit>()
            val switchStarted = CompletableDeferred<Unit>()
            val events = mutableListOf<String>()

            val decoding = async(Dispatchers.Default) {
                cache.withModel("whisper-a", { Handle("whisper-a") },
                    { events.add("release-${it.name}") }) { handle ->
                    events.add("decode-${handle.name}")
                    entered.complete(Unit)
                    finishDecode.await()
                    events.add("decode-done")
                }
            }
            entered.await()
            val switching = async(Dispatchers.Default) {
                switchStarted.complete(Unit)
                cache.withModel("whisper-b", { Handle("whisper-b") },
                    { events.add("release-${it.name}") }) {
                    events.add("decode-${it.name}")
                }
            }
            switchStarted.await()
            assertEquals(listOf("decode-whisper-a"), events)
            finishDecode.complete(Unit)
            decoding.await()
            switching.await()
            assertEquals(
                listOf("decode-whisper-a", "decode-done", "release-whisper-a", "decode-whisper-b"),
                events
            )
        }
    }

    @Test fun failedLoadDoesNotExposeReleasedHandleAndAllowsRetry() = runBlocking {
        val cache = ExclusiveModelCache<Handle>()
        val disposed = mutableListOf<String>()
        cache.warm("old", { Handle("old") }, { disposed.add(it.name) })
        try {
            cache.withModel("new", { error("model failed to load") }, { disposed.add(it.name) }) {
                error("unreachable")
            }
            throw AssertionError("Failure expected")
        } catch (e: IllegalStateException) {
            assertEquals("model failed to load", e.message)
        }
        assertEquals(listOf("old"), disposed)
        assertEquals("new", cache.withModel("new", { Handle("new") },
            { disposed.add(it.name) }) { it.name })
        assertEquals(listOf("old"), disposed)
    }
}
