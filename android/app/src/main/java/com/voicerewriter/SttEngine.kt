package com.voicerewriter

import java.io.File

/**
 * Remote transcription was retired in the local-only Android fork.
 * This compatibility stub guarantees old call sites cannot send audio to an API.
 */
@Deprecated("Cloud transcription is not supported by this Android fork")
object SttEngine {
    @Suppress("UNUSED_PARAMETER")
    suspend fun transcribe(settings: Settings, audio: File, biasPrompt: String? = null): String {
        throw UnsupportedOperationException("Cloud transcription is disabled in this Android build")
    }
}
