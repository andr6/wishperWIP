package com.voicerewriter

/**
 * In-memory FIFO of completed, not-yet-delivered transcriptions.
 *
 * Queuing is NOT delivery. The caller must explicitly acknowledge an item
 * after a successful accessibility action or clipboard write. Unacknowledged
 * entries remain retryable via their durable PendingAudio recordings after
 * process/service death. No text is logged or uploaded.
 */
internal class InsertionDeliveryQueue {
    data class Request(
        val sequence: Long,
        val text: String,
        val originalHost: String?,
        val recordingId: String?,
    )

    private val requests = ArrayDeque<Request>()
    private var nextSequence = 1L

    fun enqueue(text: String, originalHost: String?, recordingId: String?): Request {
        require(text.isNotBlank()) { "Cannot insert blank text" }
        val request = Request(nextSequence++, text, originalHost?.takeIf { it.isNotBlank() },
            recordingId?.takeIf { it.isNotBlank() })
        requests.addLast(request)
        return request
    }

    fun current(): Request? = requests.firstOrNull()

    /** Returns a delivered item exactly once. Stale/duplicate receipts do nothing. */
    fun acknowledge(sequence: Long): Request? {
        val head = requests.firstOrNull() ?: return null
        if (head.sequence != sequence) return null
        requests.removeFirst()
        return head
    }

    /** Interrupted entries are never acknowledged, and remain recoverable on disk. */
    fun abandonAll(): List<Request> {
        val old = requests.toList()
        requests.clear()
        return old
    }

    val size: Int get() = requests.size
}
