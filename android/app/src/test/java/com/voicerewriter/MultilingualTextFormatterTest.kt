package com.voicerewriter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MultilingualTextFormatterTest {
    @Test fun portuguesePunctuationSpacingAndSentenceStarts() {
        assertEquals(
            "Amanhã vamos testar o endpoint, com o João. Depois enviamos o relatório!",
            MultilingualTextFormatter.format(
                "  amanhã vamos testar o endpoint , com o João . depois enviamos o relatório !  ", "pt"
            )
        )
    }

    @Test fun spanishAccentsAndInvertedQuestionMarksRemainIntact() {
        assertEquals(
            "¿Cómo está María? ¡Bien!",
            MultilingualTextFormatter.format("  ¿cómo está María ? ¡bien !", "es")
        )
    }

    @Test fun technicalTermsNumbersAndDiacriticsAreNeverRewritten() {
        assertEquals(
            "O firewall bloqueou 1,5 GB no endpoint da CVE-2026-1234.",
            MultilingualTextFormatter.format(
                "o firewall bloqueou 1,5 GB no endpoint da CVE-2026-1234 .", "pt"
            )
        )
        assertEquals(
            "Por favor, não trocar pôr por por.",
            MultilingualTextFormatter.format("por favor , não trocar pôr por por .", "pt")
        )
    }

    @Test fun linksEmailsAndDeliberateCapitalizationArePreserved() {
        val url = "https://example.com/SensitivePath?x=1,5"
        assertEquals(url, MultilingualTextFormatter.format(url, "pt"))
        assertEquals("contacto@example.com", MultilingualTextFormatter.format("contacto@example.com", "pt"))
        assertEquals("iPhone e eBay funcionam.", MultilingualTextFormatter.format("iPhone e eBay funcionam.", "pt"))
    }

    @Test fun newlineStructureAndExistingPunctuationArePreserved() {
        assertEquals(
            "Primeira linha.\n\nSegunda linha?",
            MultilingualTextFormatter.format("primeira linha .\n\nsegunda linha ?", "pt")
        )
    }

    @Test fun automaticAndUnrelatedLanguagesAreUntouched() {
        val mixed = "vamos fazer deploy , then validate the endpoint ."
        assertEquals(mixed, MultilingualTextFormatter.format(mixed, "auto"))
        assertEquals(mixed, MultilingualTextFormatter.format(mixed, "fr"))
        assertEquals(mixed, MultilingualTextFormatter.format(mixed, "hi"))
    }

    @Test fun codeContextDoesNotChangeAnything() {
        val code = "const endpoint = 'api/v1' ; "
        assertEquals(code, MultilingualTextFormatter.format(code, "pt", isCodeContext = true))
    }

    @Test fun formatterIsIdempotent() {
        val first = MultilingualTextFormatter.format(" mañana , vamos al evento .", "es")
        assertEquals(first, MultilingualTextFormatter.format(first, "es"))
    }

    @Test fun englishOnlyPolishNeverRunsForUnknownOrAutomaticLanguage() {
        assertTrue(MultilingualTextFormatter.canUseEnglishPolish("en"))
        assertTrue(MultilingualTextFormatter.canUseEnglishPolish(" EN "))
        for (language in listOf("pt", "es", "auto", "hi", "klingon", "")) {
            assertFalse(MultilingualTextFormatter.canUseEnglishPolish(language))
        }
    }
}
