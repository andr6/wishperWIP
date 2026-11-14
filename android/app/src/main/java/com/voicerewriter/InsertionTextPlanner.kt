package com.voicerewriter

/**
 * Pure text splice used by accessibility ACTION_SET_TEXT.
 *
 * Android cursor offsets are UTF-16 code units, not Unicode code points.
 * An invalid/unknown selection in a non-empty editor or a selection that
 * splits a surrogate pair must NOT rewrite the field; the caller can try
 * ACTION_PASTE instead.
 */
internal object InsertionTextPlanner {
    data class Plan(val text: String, val cursor: Int)

    fun plan(existing: String, inserted: String, selectionStart: Int, selectionEnd: Int): Plan? {
        val (start, end) = when {
            selectionStart in 0..existing.length && selectionEnd in 0..existing.length ->
                minOf(selectionStart, selectionEnd) to maxOf(selectionStart, selectionEnd)
            existing.isEmpty() -> 0 to 0
            else -> return null
        }
        if (!isCodePointBoundary(existing, start) || !isCodePointBoundary(existing, end)) {
            return null
        }
        return Plan(
            text = existing.substring(0, start) + inserted + existing.substring(end),
            cursor = start + inserted.length,
        )
    }

    private fun isCodePointBoundary(text: String, offset: Int): Boolean =
        offset == 0 || offset == text.length ||
            !(Character.isHighSurrogate(text[offset - 1]) &&
                Character.isLowSurrogate(text[offset]))
}
