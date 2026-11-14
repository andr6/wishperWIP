package com.voicerewriter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BenchmarkMetricsTest {
    @Test fun punctuationAndCaseAreIgnored() {
        assertEquals(0.0, BenchmarkMetrics.wer("Bom dia, Lisboa!", "bom DIA lisboa"), 0.00001)
        assertEquals(0.0, BenchmarkMetrics.cer("¿Qué tal?", "qué tal"), 0.00001)
    }

    @Test fun accentsRemainSignificant() {
        assertEquals(0.5, BenchmarkMetrics.wer("Eu pôr", "eu por"), 0.00001)
        assertEquals(2.0 / 7, BenchmarkMetrics.cer("sí está", "si esta"), 0.00001)
    }

    @Test fun insertionsDeletionsAndSubstitutionsUseEditDistance() {
        assertEquals(1.0 / 3, BenchmarkMetrics.wer("um dois três", "um quatro três"), 0.00001)
        assertEquals(1.0 / 3, BenchmarkMetrics.wer("um dois três", "um três"), 0.00001)
        assertEquals(1.0 / 3, BenchmarkMetrics.wer("um dois três", "um dois três quatro"), 0.00001)
    }

    @Test fun emptyHypothesisCountsAsFullError() {
        assertEquals(1.0, BenchmarkMetrics.wer("hello world", ""), 0.00001)
        assertEquals(1.0, BenchmarkMetrics.cer("hello", ""), 0.00001)
    }

    @Test fun emptyReferenceIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { BenchmarkMetrics.wer("", "spoken") }
        assertThrows(IllegalArgumentException::class.java) { BenchmarkMetrics.cer("!!", "spoken") }
    }

    @Test fun nearestRankPercentilesAndSingleton() {
        val values = listOf(5.0, 1.0, 3.0, 2.0, 4.0)
        assertEquals(3.0, BenchmarkMetrics.percentile(values, 50), 0.00001)
        assertEquals(5.0, BenchmarkMetrics.percentile(values, 95), 0.00001)
        assertEquals(7.0, BenchmarkMetrics.percentile(listOf(7.0), 95), 0.00001)
    }
}
