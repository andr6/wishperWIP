package com.voicerewriter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DictationWorkPhaseTest {
    @Test fun distinctTranscriptionAndPolishFeedback() {
        val transcribing = DictationWorkPhase.TRANSCRIBING
        val polishing = DictationWorkPhase.POLISHING
        assertNotEquals(transcribing.bubbleLabel, polishing.bubbleLabel)
        assertNotEquals(transcribing.accessibilityDescription, polishing.accessibilityDescription)
        assertTrue(transcribing.sheetDescription.contains("recording"))
        assertTrue(polishing.sheetDescription.contains("cleanup"))
    }

    @Test fun feedbackDescribesLocalProcessingWithoutPrivateContent() {
        for (phase in DictationWorkPhase.entries) {
            assertTrue(phase.accessibilityDescription.contains("device"))
            assertTrue(phase.bubbleLabel.length <= 11)
            assertFalse(phase.accessibilityDescription.contains("cloud", ignoreCase = true))
        }
    }

    @Test fun insertedAndClipboardFeedbackAreNeverConfused() {
        assertNotEquals(DictationBubbleFeedback.INSERTED, DictationBubbleFeedback.COPIED)
        assertTrue(DictationBubbleFeedback.COPIED.contains("Paste"))
        assertTrue(DictationBubbleFeedback.INSERTED.contains("inserted"))
    }

    @Test fun repeatedMappingIsStable() {
        assertEquals("Transcribe", DictationWorkPhase.TRANSCRIBING.bubbleLabel)
        assertEquals("Polishing", DictationWorkPhase.POLISHING.bubbleLabel)
    }
}
