package com.voicerewriter

/**
 * Mandatory local-only inference policy for the Android fork.
 *
 * This is applied to every settings read and write. It accepts old cloud
 * configurations so existing installations can upgrade, but does not permit
 * those configurations or cloud credentials to reach inference call sites.
 *
 * Network access is still permitted for MODEL FILE downloads only.
 */
internal object LocalOnlyPolicy {
    fun enforce(input: Settings): Settings {
        val localLlm = input.model.takeIf { id ->
            LlmModelManager.MODELS.any { it.id == id }
        } ?: LlmModelManager.DEFAULT_MODEL
        val localStt = input.sttModel.takeIf { id ->
            OnDeviceStt.isParakeet(id) ||
                WhisperModelManager.MODELS.any { it.id == id }
        } ?: ParakeetModelManager.MODEL_ID

        return input.copy(
            provider = Defaults.DEFAULT_PROVIDER,
            sttProvider = Defaults.DEFAULT_STT_PROVIDER,
            model = localLlm,
            sttModel = localStt,
            customEndpoint = "",
            sttEndpoint = "",
            apiKey = "",
            sttKey = "",
        )
    }
}
