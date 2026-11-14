package com.voicerewriter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InsertionTextPlannerTest {
    private fun plan(text: String, insert: String, start: Int, end: Int) =
        InsertionTextPlanner.plan(text, insert, start, end)

    @Test fun insertsIntoEmptyFieldEvenWithoutSelectionMetadata() {
        assertEquals(InsertionTextPlanner.Plan("Olá, mundo!", 11),
            plan("", "Olá, mundo!", -1, -1))
    }

    @Test fun insertsAtStartMiddleAndEndWithoutDeletingExistingText() {
        assertEquals(InsertionTextPlanner.Plan("xABC", 1), plan("ABC", "x", 0, 0))
        assertEquals(InsertionTextPlanner.Plan("AxBC", 2), plan("ABC", "x", 1, 1))
        assertEquals(InsertionTextPlanner.Plan("ABCx", 4), plan("ABC", "x", 3, 3))
    }

    @Test fun replacesSelectedTextAndRestoresCursorAtEndOfInsertion() {
        assertEquals(InsertionTextPlanner.Plan("Bom dia, Maria!", 7),
            plan("Bom tarde, Maria!", "dia", 4, 9))
    }

    @Test fun reversedSelectionsHaveIdenticalBehavior() {
        assertEquals(plan("hola mundo", "equipo", 5, 10),
            plan("hola mundo", "equipo", 10, 5))
    }

    @Test fun keepsMultilingualAccentsAndLineBreaksVerbatim() {
        assertEquals(InsertionTextPlanner.Plan("Sim, amanhã.\n¡Sí, señor!", 24),
            plan("Sim, .\n¡Sí, señor!", "amanhã", 5, 5))
    }

    @Test fun unknownOrOutOfRangeSelectionInNonEmptyFieldReturnsNull() {
        assertNull(plan("existing", "x", -1, -1))
        assertNull(plan("existing", "x", 9, 9))
        assertNull(plan("existing", "x", 0, -1))
    }

    @Test fun emojiCodePointMayBeReplacedWhole() {
        val base = "A🚀B"
        assertEquals(InsertionTextPlanner.Plan("AcometaB", 7),
            plan(base, "cometa", 1, 3))
    }

    @Test fun cannotSplitUtf16SurrogatePair() {
        val base = "A🚀B"
        assertNull(plan(base, "x", 2, 2))
        assertNull(plan(base, "x", 1, 2))
        assertNull(plan(base, "x", 2, 3))
    }

    @Test fun insertionAtValidEmojiBoundaryRetainsSurrogates() {
        assertEquals(InsertionTextPlanner.Plan("A🚀!B", 4),
            plan("A🚀B", "!", 3, 3))
    }

    @Test fun exactWhitespaceIsPreserved() {
        assertEquals(InsertionTextPlanner.Plan("a  b\nc", 5),
            plan("a  c", "b\n", 3, 3))
    }
}
