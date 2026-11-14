package com.voicerewriter

import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

class BenchmarkWavValidatorTest {
    private fun fixture(): File {
        val file = File.createTempFile("asr-bench-", ".wav")
        file.deleteOnExit()
        WavIo.write(file, ShortArray(1600) { 1234 })
        return file
    }

    private fun corrupt(file: File, offset: Int, value: Int): File {
        val bytes = file.readBytes()
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).putInt(offset, value)
        file.writeBytes(bytes)
        return file
    }

    @Test fun canonicalWavIoFixturePasses() {
        BenchmarkWavValidator.validate(fixture())
    }

    @Test fun wrongSampleRateCannotMasqueradeAs16000Hz() {
        assertThrows(IllegalArgumentException::class.java) {
            BenchmarkWavValidator.validate(corrupt(fixture(), 24, 44_100))
        }
    }

    @Test fun truncatedFileIsRejected() {
        val file = fixture()
        file.writeBytes(file.readBytes().dropLast(2).toByteArray())
        assertThrows(IllegalArgumentException::class.java) {
            BenchmarkWavValidator.validate(file)
        }
    }

    @Test fun invalidRiffIsRejected() {
        val file = fixture()
        val bytes = file.readBytes()
        bytes[0] = 'X'.code.toByte()
        file.writeBytes(bytes)
        assertThrows(IllegalArgumentException::class.java) {
            BenchmarkWavValidator.validate(file)
        }
    }

    @Test fun extraChunksAreNotSilentlyDecodedAtWrongOffset() {
        val file = fixture()
        val bytes = file.readBytes().toMutableList()
        bytes.addAll(listOf(1.toByte(), 2.toByte()))
        file.writeBytes(bytes.toByteArray())
        assertThrows(IllegalArgumentException::class.java) {
            BenchmarkWavValidator.validate(file)
        }
    }
}
