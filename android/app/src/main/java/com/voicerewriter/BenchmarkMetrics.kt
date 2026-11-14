package com.voicerewriter

import java.util.Locale
import kotlin.math.ceil
import kotlin.math.min

/**
 * Deterministic, locale-neutral ASR evaluation metrics. Keep accent distinctions:
 * Portuguese "pôr" and "por" are not equivalent, nor are Spanish "sí"/"si".
 *
 * Punctuation and case are ignored for ASR comparison, but spelling and
 * diacritics are preserved. Neither text nor scores leave the device.
 */
internal object BenchmarkMetrics {
    private val punctuation = Regex("[^\\p{L}\\p{N}]+")
    private val whitespace = Regex("\\s+")

    fun normalize(text: String): String =
        text.lowercase(Locale.ROOT)
            .replace(punctuation, " ")
            .trim()
            .replace(whitespace, " ")

    fun wer(reference: String, hypothesis: String): Double {
        val expected = normalize(reference).split(' ').filter { it.isNotBlank() }
        val actual = normalize(hypothesis).split(' ').filter { it.isNotBlank() }
        require(expected.isNotEmpty()) { "Reference transcript must contain words" }
        return editDistance(expected, actual).toDouble() / expected.size
    }

    fun cer(reference: String, hypothesis: String): Double {
        val expected = normalize(reference).codePoints().toArray().toList()
        val actual = normalize(hypothesis).codePoints().toArray().toList()
        require(expected.isNotEmpty()) { "Reference transcript must contain characters" }
        return editDistance(expected, actual).toDouble() / expected.size
    }

    fun percentile(values: Collection<Double>, percentile: Int): Double {
        require(percentile in 0..100) { "Percentile must be 0..100" }
        require(values.isNotEmpty()) { "No measurements" }
        val sorted = values.sorted()
        val index = (ceil(percentile * sorted.size / 100.0).toInt() - 1).coerceIn(0, sorted.lastIndex)
        return sorted[index]
    }

    private fun <T> editDistance(expected: List<T>, actual: List<T>): Int {
        var previous = IntArray(actual.size + 1) { it }
        for (i in expected.indices) {
            val next = IntArray(actual.size + 1)
            next[0] = i + 1
            for (j in actual.indices) {
                val replace = if (expected[i] == actual[j]) 0 else 1
                next[j + 1] = min(
                    min(next[j] + 1, previous[j + 1] + 1),
                    previous[j] + replace
                )
            }
            previous = next
        }
        return previous[actual.size]
    }
}
