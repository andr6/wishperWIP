package com.voicerewriter

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/**
 * Stores cloud LLM and STT credentials encrypted at rest with a non-exportable
 * Android Keystore AES-GCM key. No dependency on AndroidX Security Crypto.
 *
 * Inaccessible/invalidated key or corrupted ciphertext is a hard error, never
 * treated as a missing key. This prevents silently accepting an empty key or
 * overwriting evidence of corruption during an upgrade.
 *
 * Android backup is disabled in the manifest. A copy of these encrypted prefs
 * alone is not sufficient for recovery on another device, by design.
 */
internal class SecureCredentials(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "secure_credentials_v1", Context.MODE_PRIVATE
    )

    private companion object {
        const val KEY_ALIAS = "com.voicerewriter.credentials.v1"
        const val LLM = "llm"
        const val STT = "stt"
    }

    private fun secretKey(create: Boolean): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore")
        store.load(null)
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        check(create) { "Stored credentials cannot be decrypted: Android Keystore key is missing" }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setKeySize(256)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    private fun encode(field: String, value: String): String =
        Base64.encodeToString(
            CredentialCipher.encrypt(secretKey(create = true), field, value),
            Base64.NO_WRAP
        )

    private fun decode(field: String, value: String): String =
        CredentialCipher.decrypt(
            secretKey(create = false),
            field,
            Base64.decode(value, Base64.NO_WRAP)
        )

    private fun read(field: String): String {
        val ciphertext = prefs.getString(field, null) ?: return ""
        return decode(field, ciphertext)
    }

    /** Retire obsolete cloud credentials without depending on a decryptable Keystore key. */
    fun clear() {
        if (!prefs.contains(LLM) && !prefs.contains(STT)) return
        check(prefs.edit().remove(LLM).remove(STT).commit()) {
            "Failed to remove obsolete cloud credentials"
        }
    }

    fun llmKey(): String = read(LLM)
    fun sttKey(): String = read(STT)

    /**
     * Write both fields in one durable SharedPreferences commit. A blank value
     * explicitly deletes that credential; a new, random GCM IV is used on write.
     */
    fun save(llm: String, stt: String) {
        val encryptedLlm = llm.takeIf { it.isNotBlank() }?.let { encode(LLM, it) }
        val encryptedStt = stt.takeIf { it.isNotBlank() }?.let { encode(STT, it) }
        val editor = prefs.edit()
        if (encryptedLlm != null) editor.putString(LLM, encryptedLlm)
        else editor.remove(LLM)
        if (encryptedStt != null) editor.putString(STT, encryptedStt)
        else editor.remove(STT)
        check(editor.commit()) { "Failed to persist encrypted credentials" }
    }

    /**
     * Idempotent upgrade from plaintext DataStore. Do not overwrite a value
     * already in the encrypted vault, even if the old DataStore copy survives
     * a crash between vault commit and DataStore cleanup.
     *
     * Existing encrypted entries are decrypted first; corruption or key loss
     * throws, so legacy plaintext is not silently promoted over bad data.
     */
    fun migrateMissing(llmLegacy: String?, sttLegacy: String?) {
        val llmPresent = prefs.contains(LLM)
        val sttPresent = prefs.contains(STT)
        if (llmPresent) llmKey()
        if (sttPresent) sttKey()
        val importLlm = CredentialMigrationPolicy.shouldImport(llmPresent, llmLegacy)
        val importStt = CredentialMigrationPolicy.shouldImport(sttPresent, sttLegacy)
        if (!importLlm && !importStt) return

        val editor = prefs.edit()
        if (importLlm) editor.putString(LLM, encode(LLM, llmLegacy!!))
        if (importStt) editor.putString(STT, encode(STT, sttLegacy!!))
        check(editor.commit()) { "Failed to persist migrated credentials" }
    }
}

/** Deterministic policy shared by Android migration and JVM tests. */
internal object CredentialMigrationPolicy {
    fun shouldImport(encryptedExists: Boolean, legacy: String?): Boolean =
        !encryptedExists && !legacy.isNullOrBlank()
}
