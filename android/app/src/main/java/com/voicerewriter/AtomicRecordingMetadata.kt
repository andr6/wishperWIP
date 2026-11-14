package com.voicerewriter

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream

/**
 * Atomic replacement of recording metadata in the same directory.
 *
 * Never truncate the existing .json in place: a failed or interrupted write
 * must leave the previous recording metadata readable. Temp files deliberately
 * end in .tmp so PendingAudio.all() cannot interpret them as recordings.
 */
internal object AtomicRecordingMetadata {
    fun writeText(target: File, value: String): Boolean =
        replace(target) { output -> output.write(value.toByteArray(Charsets.UTF_8)) }

    /** Writer injection lets JVM tests simulate full disk and interrupted writes. */
    internal fun replace(target: File, writer: (OutputStream) -> Unit): Boolean {
        val parent = target.parentFile ?: return false
        if (!parent.isDirectory && !parent.mkdirs()) return false
        var temporary: File? = null
        return try {
            val tmp = File.createTempFile(target.name + ".", ".tmp", parent)
            temporary = tmp
            FileOutputStream(tmp).use { output ->
                writer(output)
                output.fd.sync()
            }
            // Linux/Android rename within one directory replaces a file atomically.
            // If the rename fails, the prior JSON remains untouched.
            if (!tmp.renameTo(target)) throw IOException("Atomic metadata rename failed")
            true
        } catch (_: Exception) {
            false
        } finally {
            temporary?.delete()
        }
    }
}
