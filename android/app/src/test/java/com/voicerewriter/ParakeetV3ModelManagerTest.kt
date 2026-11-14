package com.voicerewriter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ParakeetV3ModelManagerTest {
    @Test fun pinnedBundleHasUniqueCompleteArtifacts() {
        assertEquals(
            setOf("tokens.txt", "decoder.int8.onnx", "joiner.int8.onnx", "encoder.int8.onnx"),
            ParakeetV3ModelManager.ASSETS.map { it.name }.toSet(),
        )
        assertEquals(4, ParakeetV3ModelManager.ASSETS.size)
        assertTrue(ParakeetV3ModelManager.ASSETS.all {
            it.bytes > 0 && it.sha256.matches(Regex("[a-f0-9]{64}"))
        })
        assertEquals(670_478_772L, ParakeetV3ModelManager.ASSETS.sumOf { it.bytes })
    }

    @Test fun v3SupportsLanguagesOfInterest() {
        for (lang in listOf("en", "pt", "es")) {
            assertTrue(ParakeetV3ModelManager.supports(lang))
        }
    }

    @Test fun v3DoesNotClaimHindiOrChinese() {
        assertFalse(ParakeetV3ModelManager.supports("hi"))
        assertFalse(ParakeetV3ModelManager.supports("zh"))
    }

    @Test fun existingV2IdentifierIsNotReused() {
        assertFalse(ParakeetV3ModelManager.MODEL_ID == ParakeetModelManager.MODEL_ID)
    }
}
