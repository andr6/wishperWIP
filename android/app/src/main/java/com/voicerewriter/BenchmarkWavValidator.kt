package com.voicerewriter

import java.io.DataInputStream
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Checks benchmark WAVs match WavIo's fixed-header, 16 kHz mono PCM16 contract.
 * Incorrect rate or additional chunks would skew accuracy and latency measurements.
 */
internal object BenchmarkWavValidator {
    private const val HEADER_BYTES = 44
    private const val MAX_AUDIO_SECONDS = 180
    private const val SAMPLE_RATE = 16_000

    fun validate(file: File) {
        require(file.isFile) { "Benchmark audio missing: ${file.name}" }
        val size = file.length()
        require(size in 46L..(HEADER_BYTES + MAX_AUDIO_SECONDS * SAMPLE_RATE * 2L)) {
            "WAV ${file.name}: require 16-kHz PCM16 mono audio between 1 sample and 180s"
        }
        val bytes = ByteArray(HEADER_BYTES)
        DataInputStream(FileInputStream(file)).use { it.readFully(bytes) }
        val header = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        fun tag(offset: Int, value: String) =
            String(bytes, offset, 4, Charsets.US_ASCII) == value
        require(
            tag(0, "RIFF") && tag(8, "WAVE") &&
                tag(12, "fmt ") && tag(36, "data")
        ) { "WAV ${file.name}: expected canonical 44-byte RIFF/WAVE header" }

        require(header.getInt(16) == 16 &&
            header.getShort(20).toInt() == 1 &&
            header.getShort(22).toInt() == 1 &&
            header.getInt(24) == SAMPLE_RATE &&
            header.getInt(28) == SAMPLE_RATE * 2 &&
            header.getShort(32).toInt() == 2 &&
            header.getShort(34).toInt() == 16
        ) { "WAV ${file.name}: expected 16-kHz mono PCM16 (no extra chunks)" }

        val dataLength = header.getInt(40).toLong() and 0xffffffffL
        val riffLength = header.getInt(4).toLong() and 0xffffffffL
        require(dataLength > 0 && dataLength % 2 == 0L &&
            riffLength == 36L + dataLength &&
            size == HEADER_BYTES + dataLength
        ) { "WAV ${file.name}: truncated, padded or inconsistent data length" }
    }
}
