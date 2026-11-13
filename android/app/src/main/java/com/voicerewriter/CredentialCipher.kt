package com.voicerewriter

import java.nio.charset.StandardCharsets
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Versioned AES-256-GCM envelope. Only ciphertext + a random IV are persisted;
 * the key itself lives in Android Keystore (see [SecureCredentials]).
 *
 * This is Android-independent so its authentication and tampering behavior can
 * be tested by the regular JVM unit-test suite.
 */
internal object CredentialCipher {
    private const val VERSION: Byte = 1
    private const val IV_SIZE = 12
    private const val TAG_BITS = 128
    private const val MIN_ENVELOPE_SIZE = 1 + IV_SIZE + TAG_BITS / 8

    private fun aad(field: String): ByteArray =
        "OpenWispr/credentials/v1/$field".toByteArray(StandardCharsets.UTF_8)

    fun encrypt(key: SecretKey, field: String, value: String): ByteArray {
        require(field == "llm" || field == "stt") { "Unknown credential field" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        check(iv.size == IV_SIZE) { "Unexpected GCM IV length" }
        cipher.updateAAD(aad(field))
        return byteArrayOf(VERSION) + iv + cipher.doFinal(value.toByteArray(StandardCharsets.UTF_8))
    }

    fun decrypt(key: SecretKey, field: String, envelope: ByteArray): String {
        require(field == "llm" || field == "stt") { "Unknown credential field" }
        require(envelope.size >= MIN_ENVELOPE_SIZE) { "Invalid credential envelope" }
        require(envelope[0] == VERSION) { "Unsupported credential envelope version" }
        val iv = envelope.copyOfRange(1, 1 + IV_SIZE)
        val ciphertext = envelope.copyOfRange(1 + IV_SIZE, envelope.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
        cipher.updateAAD(aad(field))
        return String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8)
    }
}
