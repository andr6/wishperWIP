package com.voicerewriter

/**
 * Conservative, rule-based formatting for explicitly selected European Portuguese
 * and Spanish. This is intentionally not a translation, grammar corrector, or an
 * ASR repair model: spoken words, accents, numbers, names and acronyms are kept.
 *
 * Automatic language mode cannot safely select language-specific cleanup until
 * the selected ASR engine exposes reliable language metadata to the pipeline.
 * Other manual languages likewise pass through unchanged.
 */
internal object MultilingualTextFormatter {
    private val spaceBeforePunctuation = Regex("[ \t]+([,;:.!?])")
    private val email = Regex("""\b[\p{L}\p{N}._%+-]+@[\p{L}\p{N}.-]+\.[\p{L}]{2,}\b""")

    /** Only explicit English is allowed through the current English-trained Qwen polish. */
    fun canUseEnglishPolish(language: String): Boolean =
        language.trim().equals("en", ignoreCase = true)

    fun format(raw: String, language: String, isCodeContext: Boolean = false): String {
        if (isCodeContext || !isSupported(language)) return raw
        // A line is processed independently: no new or existing paragraph breaks
        // are removed, and explicit list structure is preserved.
        return raw.split('\n').joinToString("\n") { line ->
            val spaced = line.trim(' ', '\t', '\r').replace(spaceBeforePunctuation, "$1")
            capitalizeSentencesConservatively(spaced)
        }
    }

    private fun isSupported(language: String): Boolean =
        language.trim().lowercase() in setOf("pt", "es")

    /**
     * Capitalize starts of sentences, never other words. Do not touch a line
     * containing URLs or email addresses: rewriting their case, even at the
     * start of a sentence, could corrupt a case-sensitive path or identifier.
     * A period inside a domain/decimal without following whitespace is not an
     * end-of-sentence boundary.
     */
    private fun capitalizeSentencesConservatively(line: String): String {
        if (line.contains("://") || line.contains("www.") || email.containsMatchIn(line)) {
            return line
        }
        val result = StringBuilder(line.length)
        var sentenceStart = true
        for (index in line.indices) {
            val ch = line[index]
            if (sentenceStart && ch.isLetter()) {
                // Respect words with deliberate internal capitalization (iPhone,
                // eBay, OpenAI, CVE-related identifiers and organization names).
                val tokenEnd = (index + 1 until line.length)
                    .firstOrNull { i -> !line[i].isLetterOrDigit() } ?: line.length
                val token = line.substring(index, tokenEnd)
                result.append(if (token.drop(1).any { it.isUpperCase() }) ch else ch.titlecaseChar())
                sentenceStart = false
            } else {
                result.append(ch)
                if (ch.isLetterOrDigit()) sentenceStart = false
            }
            if (ch == '.' || ch == '?' || ch == '!') {
                if (index + 1 < line.length && line[index + 1].isWhitespace()) {
                    sentenceStart = true
                }
            }
        }
        return result.toString()
    }
}
