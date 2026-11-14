package com.voicerewriter

/**
 * Settings for the floating bubble's press-and-hold shortcut.
 *
 * A tap always toggles recording. A press-and-hold is optional and only arms
 * before recording starts, so it never steals the tap-to-stop gesture.
 * Keep the default unchanged for existing installations.
 */
internal object BubbleGestureConfig {
    const val DEFAULT_HOLD_ENABLED = true
    const val DEFAULT_HOLD_DELAY_MS = 450
    const val MIN_HOLD_DELAY_MS = 300
    const val MAX_HOLD_DELAY_MS = 800
    const val STEP_MS = 50

    fun normalizeDelay(ms: Int): Int {
        val clamped = ms.coerceIn(MIN_HOLD_DELAY_MS, MAX_HOLD_DELAY_MS)
        return MIN_HOLD_DELAY_MS +
            ((clamped - MIN_HOLD_DELAY_MS + STEP_MS / 2) / STEP_MS) * STEP_MS
    }

    fun canStartHold(enabled: Boolean, moved: Boolean, recordingActive: Boolean): Boolean =
        enabled && !moved && !recordingActive
}
