package com.voicerewriter

import android.content.Context

/**
 * Routes on-device transcription to the right local engine based on the selected [Settings.sttModel]:
 * the Parakeet v2/v3 transducer (sherpa-onnx) or whisper.cpp (tiny/base/small).
 * Keeps call sites (RewriteActivity, EvalService, BubbleService) engine-agnostic.
 */
object OnDeviceStt {

    fun isParakeet(modelId: String): Boolean =
        modelId == ParakeetModelManager.MODEL_ID || modelId == ParakeetV3ModelManager.MODEL_ID

    /** V2 only understands English; V3 automatically identifies its 25 languages. */
    fun supportsLanguage(modelId: String, language: String): Boolean = when (modelId) {
        ParakeetModelManager.MODEL_ID -> DictationLanguage.isEnglish(language)
        ParakeetV3ModelManager.MODEL_ID -> DictationLanguage.isAutomatic(language) ||
            ParakeetV3ModelManager.supports(language)
        else -> true
    }

    /**
     * Resolve a stored [Settings.sttModel] to a concrete on-device model id. A blank or
     * unrecognized value (e.g. a fresh install, where the field is empty) falls back to the
     * local provider's default — Parakeet — so the recommended engine is used by default.
     *
     * An explicit language must be supported by the selected Parakeet version.
     * Automatic detection is supported by Parakeet v3 and multilingual Whisper;
     * Parakeet v2 is English-only and routes automatic input to Whisper instead.
     * The same routing applies to warm-up, readiness checks, and transcription.
     *
     * Whisper's largest downloaded size is preferred, because Whisper's non-English accuracy
     * falls off much faster than its English does — `tiny` in Hindi is not worth shipping if
     * `small` is already on disk.
     */
    @JvmOverloads
    fun resolveModel(modelId: String, language: String = DictationLanguage.DEFAULT): String {
        val known = isParakeet(modelId) || WhisperModelManager.MODELS.any { it.id == modelId }
        val id = if (known) modelId else Defaults.STT_PROVIDERS.getValue("local").defaultModel
        // A language-specific Whisper transliteration fine-tune pins its decoder
        // language internally and cannot honestly implement auto-detection.
        if (DictationLanguage.isAutomatic(language) && !isParakeet(id) &&
            WhisperModelManager.model(id).isTransliterating) {
            return WhisperModelManager.DEFAULT_MODEL
        }
        if (!isParakeet(id) || supportsLanguage(id, language)) return id
        return WhisperModelManager.DEFAULT_MODEL
    }

    /**
     * Same as [resolveModel] but able to see what is actually on disk, so a user of an unsupported language who has
     * `small` downloaded gets `small` rather than the registry default. Falls back to the
     * default when nothing better is present — the caller still has to handle "not downloaded".
     */
    fun resolveModel(context: Context, modelId: String, language: String): String {
        val id = resolveModel(modelId, language)
        // Auto mode cannot use a fixed-language Whisper fine-tune. Prefer a
        // general-purpose multilingual Whisper already present on the device.
        if (DictationLanguage.isAutomatic(language) && !isParakeet(modelId) &&
            WhisperModelManager.model(modelId).isTransliterating) {
            return WhisperModelManager.GENERIC.lastOrNull {
                WhisperModelManager.isReady(context, it.id)
            }?.id ?: id
        }
        if (!isParakeet(modelId) || supportsLanguage(modelId, language)) return id
        // Biggest-first: GENERIC is ordered tiny -> small, and accuracy tracks size. It excludes
        // the transliterating fine-tunes on purpose: the Hinglish model emits Roman script, so
        // handing it a Tamil or Marathi dictation would produce confident nonsense.
        return WhisperModelManager.GENERIC.lastOrNull { WhisperModelManager.isReady(context, it.id) }?.id ?: id
    }

    /**
     * Is the engine this user would actually transcribe with downloaded? [language] matters:
     * a Hindi user with Parakeet selected is really waiting on a Whisper model, and answering
     * about Parakeet would send them into a dictation that cannot run.
     */
    @JvmOverloads
    fun isReady(context: Context, modelId: String, language: String = DictationLanguage.DEFAULT): Boolean =
        resolveModel(context, modelId, language).let { id ->
            if (id == ParakeetModelManager.MODEL_ID) ParakeetModelManager.isReady(context)
            else if (id == ParakeetV3ModelManager.MODEL_ID) ParakeetV3ModelManager.isReady(context)
            else WhisperModelManager.isReady(context, id)
        }

    suspend fun transcribe(context: Context, settings: Settings, samples: FloatArray, biasPrompt: String? = null): String {
        val id = resolveModel(context, settings.sttModel, settings.sttLanguage)
        val s = if (id != settings.sttModel) settings.copy(sttModel = id) else settings
        return if (isParakeet(id)) LocalParakeetStt.transcribe(context, s, samples, biasPrompt)
        else LocalWhisperStt.transcribe(context, s, samples, biasPrompt)
    }

    @JvmOverloads
    suspend fun warm(
        context: Context,
        modelId: String,
        biasPrompt: String? = null,
        language: String = DictationLanguage.DEFAULT,
    ) = resolveModel(context, modelId, language).let { id ->
        if (isParakeet(id)) LocalParakeetStt.warm(context, biasPrompt, id)
        else LocalWhisperStt.warm(context, id)
    }
}
