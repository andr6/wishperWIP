package com.voicerewriter

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.GeneralSecurityException
import javax.crypto.spec.SecretKeySpec

class CredentialCipherTest {
    private val key = SecretKeySpec(ByteArray(32) { (it * 7 + 3).toByte() }, "AES")

    @Test fun encryptDecryptRoundTripWithUnicode() {
        val value = "chave-🔑-português-español"
        val encrypted = CredentialCipher.encrypt(key, "llm", value)
        assertFalse(encrypted.contentEquals(value.toByteArray(Charsets.UTF_8)))
        assertEquals(value, CredentialCipher.decrypt(key, "llm", encrypted))
    }

    @Test fun eachWriteUsesUniqueIv() {
        val a = CredentialCipher.encrypt(key, "stt", "secret")
        val b = CredentialCipher.encrypt(key, "stt", "secret")
        assertFalse(a.contentEquals(b))
    }

    @Test fun tamperingCannotBeDecrypted() {
        val encrypted = CredentialCipher.encrypt(key, "llm", "secret")
        encrypted[encrypted.lastIndex] = (encrypted.last().toInt() xor 1).toByte()
        assertThrows(GeneralSecurityException::class.java) {
            CredentialCipher.decrypt(key, "llm", encrypted)
        }
    }

    @Test fun wrongCredentialFieldIsRejected() {
        val encrypted = CredentialCipher.encrypt(key, "llm", "secret")
        assertThrows(GeneralSecurityException::class.java) {
            CredentialCipher.decrypt(key, "stt", encrypted)
        }
    }

    @Test fun wrongKeyIsRejected() {
        val encrypted = CredentialCipher.encrypt(key, "llm", "secret")
        val other = SecretKeySpec(ByteArray(32) { 0 }, "AES")
        assertThrows(GeneralSecurityException::class.java) {
            CredentialCipher.decrypt(other, "llm", encrypted)
        }
    }

    @Test fun malformedAndUnknownVersionsAreRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            CredentialCipher.decrypt(key, "stt", byteArrayOf(1, 2))
        }
        val encrypted = CredentialCipher.encrypt(key, "stt", "secret")
        encrypted[0] = 42
        assertThrows(IllegalArgumentException::class.java) {
            CredentialCipher.decrypt(key, "stt", encrypted)
        }
    }

    @Test fun migrationOnlyImportsMissingNonBlankValues() {
        assertTrue(CredentialMigrationPolicy.shouldImport(false, "old-key"))
        assertFalse(CredentialMigrationPolicy.shouldImport(true, "old-key"))
        assertFalse(CredentialMigrationPolicy.shouldImport(false, null))
        assertFalse(CredentialMigrationPolicy.shouldImport(false, ""))
        assertFalse(CredentialMigrationPolicy.shouldImport(false, "  "))
    }
}
