package com.voicerewriter

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

/**
 * Opt-in Parakeet TDT 0.6B v3 (25 European languages), stored independently of
 * the existing English-only Parakeet v2 bundle and Whisper model weights.
 *
 * All four assets are pinned to a specific Hugging Face commit and checked
 * against the published SHA-256 and exact file size. The small .verified marker
 * prevents treating half-downloaded model directories as ready.
 */
object ParakeetV3ModelManager {
    const val MODEL_ID = "parakeet-v3"
    const val LABEL = "Parakeet v3 · 25 European languages (preview)"
    const val SIZE_LABEL = "~671 MB"
    private const val REPO = "csukuangfj/sherpa-onnx-nemo-parakeet-tdt-0.6b-v3-int8"
    private const val REVISION = "876ff91b4ab4b89c328afdb2b27ff879d3e42f87"
    private const val MARKER = ".verified-v1"

    data class Asset(val name: String, val bytes: Long, val sha256: String)

    // Same ONNX export as the official sherpa-onnx model. Tokens are part of
    // this exact bundle and MUST NOT be mixed with v2 weights.
    val ASSETS = listOf(
        Asset("tokens.txt", 93939L,
            "d58544679ea4bc6ac563d1f545eb7d474bd6cfa467f0a6e2c1dc1c7d37e3c35d"),
        Asset("decoder.int8.onnx", 11845275L,
            "179e50c43d1a9de79c8a24149a2f9bac6eb5981823f2a2ed88d655b24248db4e"),
        Asset("joiner.int8.onnx", 6355277L,
            "3164c13fc2821009440d20fcb5fdc78bff28b4db2f8d0f0b329101719c0948b3"),
        Asset("encoder.int8.onnx", 652184281L,
            "acfc2b4456377e15d04f0243af540b7fe7c992f8d898d751cf134c3a55fd2247"),
    )

    /** These are the languages listed in the official sherpa-onnx v3 model documentation. */
    val SUPPORTED_LANGUAGES = setOf(
        "bg", "hr", "cs", "da", "nl", "en", "et", "fi", "fr",
        "de", "el", "hu", "it", "lv", "lt", "mt", "pl", "pt",
        "ro", "sk", "sl", "es", "sv", "ru", "uk",
    )

    fun supports(language: String): Boolean =
        DictationLanguage.normalize(language) in SUPPORTED_LANGUAGES

    fun modelDir(context: Context): File =
        File(File(context.filesDir, "models"), MODEL_ID).apply { mkdirs() }

    fun file(context: Context, name: String): File = File(modelDir(context), name)
    fun encoderPath(context: Context) = file(context, "encoder.int8.onnx").absolutePath
    fun decoderPath(context: Context) = file(context, "decoder.int8.onnx").absolutePath
    fun joinerPath(context: Context) = file(context, "joiner.int8.onnx").absolutePath
    fun tokensPath(context: Context) = file(context, "tokens.txt").absolutePath

    fun isReady(context: Context): Boolean {
        val dir = modelDir(context)
        val marker = File(dir, MARKER)
        if (!marker.isFile || marker.readText() != REVISION) return false
        return ASSETS.all { File(dir, it.name).length() == it.bytes }
    }

    private fun sourceUrl(name: String): String =
        "https://huggingface.co/$REPO/resolve/$REVISION/$name"

    /** Verify a file on an IO worker; never hash a 650 MB file on the UI thread. */
    private fun verified(target: File, asset: Asset): Boolean {
        if (!target.isFile || target.length() != asset.bytes) return false
        val digest = MessageDigest.getInstance("SHA-256")
        target.inputStream().buffered().use { input ->
            val buf = ByteArray(1 shl 16)
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                digest.update(buf, 0, n)
            }
        }
        val actual = digest.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) }
        return actual == asset.sha256
    }

    suspend fun download(context: Context, onProgress: (Float) -> Unit) = withContext(Dispatchers.IO) {
        if (isReady(context)) { onProgress(1f); return@withContext }
        val dir = modelDir(context)
        File(dir, MARKER).delete()
        for (asset in ASSETS) {
            val target = File(dir, asset.name)
            if (verified(target, asset)) continue
            // Keep valid .part/.etag sidecars from an interrupted download so
            // ModelDownloader can resume a ~652 MB transfer. Only clear a
            // completed target that fails digest or size verification.
            if (target.exists()) ModelDownloader.deleteWithSidecars(target)
            ModelDownloader.fetch(sourceUrl(asset.name), target) { p ->
                if (asset.name == "encoder.int8.onnx") onProgress(p)
            }
            if (!verified(target, asset)) {
                ModelDownloader.deleteWithSidecars(target)
                throw IllegalStateException("${asset.name}: pinned model checksum/size mismatch")
            }
        }
        val tmp = File(dir, "$MARKER.tmp")
        tmp.writeText(REVISION)
        check(tmp.renameTo(File(dir, MARKER))) { "Unable to finalize verified Parakeet v3 bundle" }
        check(isReady(context)) { "Parakeet v3 bundle incomplete" }
        Log.i("ParakeetV3", "verified model bundle ready")
        onProgress(1f)
    }

    suspend fun delete(context: Context): Long = withContext(Dispatchers.IO) {
        val dir = modelDir(context)
        File(dir, MARKER).delete()
        File(dir, "$MARKER.tmp").delete()
        ASSETS.sumOf { ModelDownloader.deleteWithSidecars(File(dir, it.name)) }
    }
}
