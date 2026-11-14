package com.voicerewriter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalOnlyPolicyTest {
    @Test fun legacyCloudProvidersAreForcedToLocalWithoutCredentials() {
        val legacy = Settings(
            provider = "openrouter",
            model = "anthropic/remote-model",
            customEndpoint = "https://example.invalid/v1/chat/completions",
            apiKey = "sensitive-llm-key",
            sttProvider = "groq",
            sttModel = "whisper-large-v3-turbo",
            sttEndpoint = "https://example.invalid/audio/transcriptions",
            sttKey = "sensitive-stt-key",
        )
        val local = LocalOnlyPolicy.enforce(legacy)
        assertEquals("local", local.provider)
        assertEquals("local", local.sttProvider)
        assertEquals(LlmModelManager.DEFAULT_MODEL, local.model)
        assertEquals(ParakeetModelManager.MODEL_ID, local.sttModel)
        assertTrue(local.customEndpoint.isEmpty())
        assertTrue(local.sttEndpoint.isEmpty())
        assertTrue(local.apiKey.isEmpty())
        assertTrue(local.sttKey.isEmpty())
        assertFalse(local.isConfigured)
        assertFalse(local.isSttConfigured)
    }

    @Test fun existingLocalModelsAndPreferencesArePreserved() {
        val model = WhisperModelManager.MODELS.first().id
        val input = Settings(
            provider = "local",
            model = LlmModelManager.MODELS.last().id,
            sttProvider = "local",
            sttModel = model,
            sttLanguage = "pt",
            voice = "European Portuguese",
            vadAutoStop = false,
            reviewBeforeInsert = true,
        )
        val actual = LocalOnlyPolicy.enforce(input)
        assertEquals(input, actual)
    }

    @Test fun normalizationIsIdempotent() {
        val input = Settings(provider = "anthropic", sttProvider = "openai")
        val once = LocalOnlyPolicy.enforce(input)
        assertEquals(once, LocalOnlyPolicy.enforce(once))
    }

    @Test fun onDeviceProviderRegistryContainsOnlyLocalChoices() {
        assertEquals(setOf("local"), Defaults.PROVIDERS.keys)
        assertEquals(setOf("local"), Defaults.STT_PROVIDERS.keys)
    }
}
