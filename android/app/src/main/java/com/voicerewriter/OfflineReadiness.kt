package com.voicerewriter

/**
 * Explains offline readiness without starting downloads or inference.
 *
 * The effective STT model is already resolved by OnDeviceStt, taking the
 * current language and any installed multilingual Whisper fallback into account.
 * AI polish is optional and only used for explicitly selected English dictation.
 */
internal object OfflineReadiness {
    data class Status(
        val canDictateOffline: Boolean,
        val speechModel: String,
        val speechMessage: String,
        val speechAction: String?,
        val polishAvailable: Boolean,
        val polishMessage: String,
        val polishAction: String?,
    )

    fun evaluate(
        selectedModel: String,
        effectiveModel: String,
        language: String,
        speechInstalled: Boolean,
        polishEnabled: Boolean,
        polishInstalled: Boolean,
        polishModel: String,
    ): Status {
        require(effectiveModel.isNotBlank()) { "A resolved speech model is required" }
        val speech = when {
            !speechInstalled -> "Speech model not installed. Download ${label(effectiveModel)} while online to dictate offline."
            effectiveModel != selectedModel && selectedModel.isNotBlank() ->
                "Ready offline with ${label(effectiveModel)} (language-compatible fallback from ${label(selectedModel)})."
            else -> "Ready offline with ${label(effectiveModel)}."
        }
        val polishSupported = language.trim().equals("en", ignoreCase = true)
        val polishMessage = when {
            !polishEnabled -> "AI polish is off. Dictation works without a cleanup model."
            !polishSupported -> "AI polish is not used for this language. Dictation remains local."
            polishInstalled -> "Optional local AI polish is installed and available offline."
            else -> "Optional AI polish model is missing; dictation still works with deterministic cleanup."
        }
        return Status(
            canDictateOffline = speechInstalled,
            speechModel = effectiveModel,
            speechMessage = speech,
            speechAction = if (speechInstalled) null else effectiveModel,
            polishAvailable = polishEnabled && polishSupported && polishInstalled,
            polishMessage = polishMessage,
            polishAction = if (polishEnabled && polishSupported && !polishInstalled) polishModel else null,
        )
    }

    private fun label(id: String): String = when (id) {
        ParakeetModelManager.MODEL_ID -> "Parakeet v2"
        ParakeetV3ModelManager.MODEL_ID -> "Parakeet v3"
        else -> WhisperModelManager.MODELS.firstOrNull { it.id == id }?.label ?: id
    }
}
