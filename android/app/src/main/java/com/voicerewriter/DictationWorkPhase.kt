package com.voicerewriter

/**
 * Privacy-preserving language for the two compute phases after microphone capture.
 * These labels describe real actions, never fabricated progress percentages.
 * No dictated words, filenames, prompts or model names are exposed in overlays.
 */
internal enum class DictationWorkPhase(
    val bubbleLabel: String,
    val accessibilityDescription: String,
    val sheetDescription: String,
) {
    TRANSCRIBING(
        "Transcribe",
        "Transcribing recorded speech on this device",
        "Converting the recording to text with your downloaded speech model.",
    ),
    POLISHING(
        "Polishing",
        "Polishing transcription locally on this device",
        "Applying optional on-device text cleanup. Your words stay on the phone.",
    ),
}

/**
 * Lightweight status strings for accessibility services. Labels intentionally
 * contain no user content, and do not assert that insertion has succeeded until
 * the accessibility insertion callback actually confirms it.
 */
internal object DictationBubbleFeedback {
    const val IDLE = "Start dictation. Tap to record; hold to talk if enabled."
    const val LISTENING = "Recording speech. Tap the bubble to stop."
    const val INSERTED = "Dictation inserted in the selected text field."
    const val COPIED = "Dictation copied to clipboard. Paste it into the text field."
}
