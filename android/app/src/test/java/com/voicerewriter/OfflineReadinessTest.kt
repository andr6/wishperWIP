package com.voicerewriter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineReadinessTest {
    @Test fun missingSpeechBlocksOfflineDictationEvenIfPolishIsReady() {
        val s = OfflineReadiness.evaluate("base", "base", "pt", false, true, true, "openwispr-qwen3-0.6b")
        assertFalse(s.canDictateOffline)
        assertEquals("base", s.speechAction)
        assertNull(s.polishAction)
    }

    @Test fun installedMultilingualFallbackIsReadyEvenIfV2WasSelected() {
        val s = OfflineReadiness.evaluate("parakeet", "small", "pt", true, true, false, "openwispr-qwen3-0.6b")
        assertTrue(s.canDictateOffline)
        assertTrue(s.speechMessage.contains("fallback"))
        assertTrue(s.speechMessage.contains("Small"))
        assertNull(s.speechAction)
    }

    @Test fun missingOptionalEnglishPolishDoesNotBlockSpeech() {
        val s = OfflineReadiness.evaluate("parakeet", "parakeet", "en", true, true, false, "openwispr-qwen3-0.6b")
        assertTrue(s.canDictateOffline)
        assertFalse(s.polishAvailable)
        assertEquals("openwispr-qwen3-0.6b", s.polishAction)
    }

    @Test fun installedEnglishPolishIsOptionalAndReady() {
        val s = OfflineReadiness.evaluate("parakeet-v3", "parakeet-v3", "en", true, true, true, "openwispr-qwen3-0.6b")
        assertTrue(s.polishAvailable)
        assertNull(s.polishAction)
    }

    @Test fun autoAndSpanishNeverAdvertiseEnglishPolish() {
        for (lang in listOf("auto", "pt", "es", "hi")) {
            val s = OfflineReadiness.evaluate("base", "base", lang, true, true, false, "qwen3-0.6b")
            assertNull(s.polishAction)
            assertFalse(s.polishAvailable)
        }
    }

    @Test fun disabledPolishNeverRequestsDownload() {
        val s = OfflineReadiness.evaluate("base", "base", "en", true, false, false, "qwen3-0.6b")
        assertNull(s.polishAction)
        assertTrue(s.canDictateOffline)
    }
}
