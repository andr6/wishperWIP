package com.voicerewriter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InsertionDeliveryQueueTest {
    @Test fun enqueueDoesNotPretendToBeDelivery() {
        val queue = InsertionDeliveryQueue()
        val r = queue.enqueue("Olá, Coimbra!", "com.example.chat", "rec1")
        assertEquals(r, queue.current())
        assertEquals(1, queue.size)
    }

    @Test fun simultaneousRequestsRetainFifoOrderingAndSeparateAudioIds() {
        val q = InsertionDeliveryQueue()
        val a = q.enqueue("primeiro", "com.example.one", "rec1")
        val b = q.enqueue("segundo", "com.example.two", "rec2")
        assertEquals(a, q.current())
        assertNull(q.acknowledge(b.sequence)) // cannot skip or steal newer request
        assertEquals(2, q.size)
        assertEquals(a, q.acknowledge(a.sequence))
        assertEquals(b, q.current())
        assertEquals(b, q.acknowledge(b.sequence))
        assertNull(q.current())
    }

    @Test fun staleOrDuplicateReceiptsCannotCompleteTheNextRecording() {
        val q = InsertionDeliveryQueue()
        val a = q.enqueue("a", null, "rec1")
        val b = q.enqueue("b", null, "rec2")
        assertEquals(a, q.acknowledge(a.sequence))
        assertNull(q.acknowledge(a.sequence))
        assertEquals(b, q.current())
        assertEquals(b, q.acknowledge(b.sequence))
    }

    @Test fun interruptionsReturnAllUnacknowledgedRecordingIds() {
        val q = InsertionDeliveryQueue()
        q.enqueue("abc", "com.example", "a")
        q.enqueue("def", "com.example", "b")
        assertEquals(listOf("a", "b"), q.abandonAll().mapNotNull { it.recordingId })
        assertTrue(q.abandonAll().isEmpty())
        assertNull(q.current())
    }

    @Test fun nonRecordingClipboardFlowRemainsSupported() {
        val q = InsertionDeliveryQueue()
        val r = q.enqueue("texto", " ", "")
        assertNull(r.originalHost)
        assertNull(r.recordingId)
        assertEquals(r, q.acknowledge(r.sequence))
    }
}
