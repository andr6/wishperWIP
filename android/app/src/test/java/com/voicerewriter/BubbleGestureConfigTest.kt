package com.voicerewriter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BubbleGestureConfigTest {
    @Test fun originalBubbleBehaviorRemainsDefault() {
        assertTrue(BubbleGestureConfig.DEFAULT_HOLD_ENABLED)
        assertEquals(450, BubbleGestureConfig.DEFAULT_HOLD_DELAY_MS)
    }

    @Test fun holdActivationDelayIsBoundedAndRounded() {
        val config = BubbleGestureConfig
        assertEquals(300, config.normalizeDelay(-100))
        assertEquals(800, config.normalizeDelay(9999))
        assertEquals(450, config.normalizeDelay(450))
        assertEquals(600, config.normalizeDelay(576))
        assertEquals(550, config.normalizeDelay(574))
    }

    @Test fun disablingHoldNeverStartsPushToTalk() {
        assertFalse(BubbleGestureConfig.canStartHold(false, moved = false, recordingActive = false))
    }

    @Test fun draggingOrRecordingPreventsLongHoldShortcut() {
        assertFalse(BubbleGestureConfig.canStartHold(true, moved = true, recordingActive = false))
        assertFalse(BubbleGestureConfig.canStartHold(true, moved = false, recordingActive = true))
        assertTrue(BubbleGestureConfig.canStartHold(true, moved = false, recordingActive = false))
    }
}
