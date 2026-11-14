package com.voicerewriter

/**
 * Prevent inserting dictated text into an unrelated app if the user switches
 * focus while local ASR or text cleanup is working. Unknown original hosts
 * retain the previous non-self fallback behavior, but system surfaces are
 * never treated as normal dictation targets.
 */
internal object InsertionTargetPolicy {
    private val excluded = setOf(
        "com.android.systemui", "android", "com.android.launcher",
        "com.sec.android.app.launcher",
    )

    fun allows(
        expectedHost: String?,
        rootPackage: CharSequence?,
        focusedPackage: CharSequence?,
        ownPackage: String,
    ): Boolean {
        val root = rootPackage?.toString()?.trim().orEmpty()
        if (root.isBlank() || root == ownPackage || root in excluded) return false
        val focused = focusedPackage?.toString()?.trim().orEmpty()
        if (focused.isNotEmpty() && focused != root) return false
        val expected = expectedHost?.trim().orEmpty()
        return expected.isBlank() || expected == root
    }
}
