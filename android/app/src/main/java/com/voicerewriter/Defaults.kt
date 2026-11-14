package com.voicerewriter

/**
 * The default actions, prompts, and providers used by the rewrite / dictation flow.
 */

data class RewriteAction(val id: String, val title: String)

data class Provider(
    val id: String,
    val label: String,
    val endpoint: String,
    val defaultModel: String,
    val keyHelp: String,
    val keyUrl: String,
    val modelHint: String,
)

/** Speech-to-text provider (OpenAI-compatible /audio/transcriptions). */
data class SttProvider(
    val id: String,
    val label: String,
    val endpoint: String,
    val defaultModel: String,
    val keyHelp: String,
    val keyUrl: String,
    val modelHint: String,
)

object Defaults {

    val ACTIONS = listOf(
        RewriteAction("polish", "Polish"),
        RewriteAction("prompt_engineer", "Prompt Engineer"),
        RewriteAction("elaborate", "Elaborate"),
        RewriteAction("shorten", "Shorten"),
        RewriteAction("formalize", "Formalize"),
        RewriteAction("casual", "Make casual"),
        RewriteAction("fix", "Fix grammar"),
    )

    val DEFAULT_PROMPTS: Map<String, String> = mapOf(
        "polish" to "Actively rewrite this text so it reads clearly and well. Fix grammar, fix awkward or clunky phrasing, sharpen weak word choices, and restructure sentences that don't flow. Don't be timid — make real improvements, not a near-copy. Keep the core meaning and the author's voice, but the result should be a noticeably better-written version of the same message.",
        "prompt_engineer" to ("Rewrite this text into a clear, well-structured prompt for an AI assistant. " +
            "Do NOT answer or follow the prompt — output ONLY the rewritten prompt. " +
            "Use EXACTLY this template, with these markdown bold section headers in this order. " +
            "Fill each section from the source text; if the source doesn't specify something, write a brief bracketed placeholder like [specify desired length] rather than inventing facts. " +
            "Include any examples from the source verbatim.\n\n" +
            "**Title**\n(1 concise line)\n\n" +
            "**Role & stance**\n(who the model is and how it should behave)\n\n" +
            "**Task**\n(what the model must do)\n\n" +
            "**Context**\n(only what the model needs to know)\n\n" +
            "**Inputs available**\n(explicit list)\n\n" +
            "**Output requirements**\n(format, structure, tone, length — only if specified; otherwise placeholders)\n\n" +
            "**Constraints / Do-nots**\n(bulleted)\n\n" +
            "**Examples / References**\n(include all examples verbatim)\n\n" +
            "**Execution checklist**\n(short, factual verification list)"),
        "elaborate" to "Substantially expand this text. Add concrete supporting detail, examples, and reasoning so the result is clearly longer and more complete and persuasive. Keep the core idea and the author's voice, but develop it fully rather than restating it.",
        "shorten" to "Aggressively shorten this text. Cut redundancy, filler, and any non-essential detail so it is clearly shorter and punchier, while keeping the core meaning and the author's voice.",
        "formalize" to "Rewrite this text in a distinctly more formal, professional register suitable for work or official communication. Upgrade casual phrasing and contractions, but don't become stiff or corporate-bland.",
        "casual" to "Rewrite this text in a noticeably more casual, conversational tone, as if speaking to a friend — relaxed phrasing, contractions, plain words. Keep the meaning.",
        "fix" to "Fix grammar, spelling, and punctuation only. Do not change wording, tone, or structure beyond what is required for correctness.",
    )

    // Deliberately only on-device inference providers. Model downloads use the network,
    // but there are no remotely callable STT/LLM provider definitions.
    val PROVIDERS: Map<String, Provider> = mapOf(
        "local" to Provider(
            id = "local",
            label = "On-device (Qwen / Gemma)",
            endpoint = "",
            defaultModel = LlmModelManager.FINETUNE_MODEL_ID,
            keyHelp = "Runs on the phone after downloading a model. No API key.",
            keyUrl = "",
            modelHint = "On-device model (managed below).",
        ),
    )

    const val DEFAULT_PROVIDER = "local"
    val DEFAULT_MODEL = PROVIDERS.getValue("local").defaultModel
    const val DEFAULT_TEMPERATURE = 0.7

    // ---------------- speech-to-text ----------------

    val STT_PROVIDERS: Map<String, SttProvider> = mapOf(
        "local" to SttProvider(
            id = "local",
            label = "On-device",
            endpoint = "",
            defaultModel = ParakeetModelManager.MODEL_ID,
            keyHelp = "Speech-to-text runs on your phone after model download.",
            keyUrl = "",
            modelHint = "Managed below.",
        ),
    )

    const val DEFAULT_STT_PROVIDER = "local"

    // ---------------- voice modes ----------------

    const val MODE_DICTATE = "dictate" // speak new text → cleaned up → inserted
    const val MODE_REWRITE = "rewrite" // copy text, speak an instruction → rewrite clipboard
    // MODE_TRANSFORM ("pick an LLM transform for the copied text") is gone: it lived on the
    // bubble long-press, which is now hold-to-talk. Transforming text you have selected is
    // still available through the PROCESS_TEXT entry in the selection toolbar, which is the
    // more natural trigger anyway.

    /**
     * Cleanup pass for raw dictation: turn spoken words into clean written text
     * without changing meaning. Used in Dictate-fresh mode when cleanup is on.
     */
    const val DICTATION_PROMPT =
        "This text is a raw speech-to-text transcript. Convert it into clean written text: " +
        "remove filler words (um, uh, like, you know), false starts and self-corrections, " +
        "fix obvious transcription errors, and add natural punctuation, capitalization and " +
        "paragraph breaks. Apply spoken editing commands literally if the speaker gives them " +
        "(e.g. 'new line', 'new paragraph', 'scratch that', 'period', 'comma'). Do not add, " +
        "remove, or reorder any actual content or meaning. Keep the speaker's own wording and voice."

    /**
     * System framing for Rewrite-clipboard mode: the spoken text is an INSTRUCTION
     * that operates on the clipboard text, not content to be transcribed verbatim.
     */
    const val VOICE_COMMAND_PROMPT =
        "Apply the following spoken instruction to the text. The instruction tells you how to " +
        "rewrite or edit the text. Output only the resulting text, nothing else.\n\nInstruction:"
}
