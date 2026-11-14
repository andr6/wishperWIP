package com.voicerewriter

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InsertionTargetPolicyTest {
    private val self = "com.voicerewriter"

    private fun mayInsert(expected: String?, root: String?, focused: String? = root): Boolean =
        InsertionTargetPolicy.allows(expected, root, focused, self)

    @Test fun acceptsTheOriginalDictationHostOnly() {
        assertTrue(mayInsert("com.example.mail", "com.example.mail"))
        assertFalse(mayInsert("com.example.mail", "com.example.chat"))
    }

    @Test fun appSwitchDuringDecodingCannotDeliverToAnotherFocusedApp() {
        assertFalse(mayInsert("com.example.mail", "com.example.chat", "com.example.chat"))
        assertTrue(mayInsert("com.example.mail", "com.example.mail", "com.example.mail"))
    }

    @Test fun neverInsertsIntoOwnActivityOrSystemSurfaces() {
        assertFalse(mayInsert(null, self))
        assertFalse(mayInsert(null, "com.android.systemui"))
        assertFalse(mayInsert(null, "android"))
        assertFalse(mayInsert(null, "com.android.launcher"))
        assertFalse(mayInsert(null, "com.sec.android.app.launcher"))
    }

    @Test fun missingRootCannotBeTrusted() {
        assertFalse(mayInsert("com.example.mail", null))
        assertFalse(mayInsert(null, ""))
    }

    @Test fun focusedNodeFromAnotherPackageIsRejected() {
        assertFalse(mayInsert("com.example.mail", "com.example.mail", "com.example.other"))
    }

    @Test fun blankFocusedNodePackageUsesTrustedRoot() {
        assertTrue(mayInsert("com.example.mail", "com.example.mail", null))
    }

    @Test fun missingOriginalHostRetainsNonSystemFallback() {
        assertTrue(mayInsert(null, "com.example.mail"))
        assertTrue(mayInsert("  ", "com.example.chat"))
    }
}
