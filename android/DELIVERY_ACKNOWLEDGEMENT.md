# P3 — Delivery acknowledgement and overlapping dictation safety

The Android dictation pipeline must distinguish **queuing text** from
**actually delivering text**. Previously `RewriteActivity.acceptVoice()`
settled its stored WAV immediately after queueing accessibility insertion.
A stopped accessibility service or lost editor could therefore leave neither
a delivered insertion nor a retryable recording.

## Implementation

- `InsertionDeliveryQueue` keeps insertion requests in FIFO order. Every
  entry carries the text, original host package and durable recording ID.
  Later recordings cannot overwrite an earlier pending text insertion.
- Only successful `ACTION_SET_TEXT` / `ACTION_PASTE`, or a confirmed final
  clipboard copy, acknowledges an entry. Duplicate or stale sequence numbers
  cannot acknowledge the wrong recording.
- After acknowledgement, `PendingAudio.settle()` runs off the UI thread,
  so the recording is not marked terminal before delivery.
- If the accessibility service is interrupted, its unacknowledged entries
  are released to Home's retry list. A process crash naturally clears its
  volatile queue; the recordings remain unsettled on disk.
- If the clipboard fallback throws, the item is not settled and becomes
  retryable. Subsequent queued items can still proceed.
- When accessibility was disabled, the direct clipboard path is unchanged;
  it settles its WAV only after `setClipboard()` succeeds.
- Existing insertion host-affinity and Unicode cursor protections are
  preserved. No audio or transcripts are sent to a server.

## Regression coverage

`InsertionDeliveryQueueTest` verifies the initial queued state, distinct
recording IDs, FIFO completion, stale/duplicate acknowledgements, discarded
in-memory work after an interruption, and non-recording clipboard flows.

## Remaining device acceptance

- [ ] CI compiles debug APK/instrumentation APK and runs JVM unit tests.
- [ ] Dictate in App A, switch to App B: never insert into B; if A is not
      restored, copy to the clipboard and then acknowledge the recording.
- [ ] Queue two local dictations while the first is waiting for host focus.
      Both must be delivered in order and neither recording may be lost.
- [ ] Disable/restart the accessibility service while a delivery is pending.
      Its WAV must remain available as unfinished/retryable.
- [ ] Confirm a failed clipboard write does not delete or settle WAV audio.
- [ ] Verify successful insertion and clipboard fallback each settle once,
      while duplicated/stale callbacks do not settle a later recording.
- [ ] Confirm normal tap/hold recording and review/insertion, with network
      access disabled after model provisioning.

**Limitations:** In-memory FIFO scheduling cannot survive process death; this
is intentional. Recovery uses the on-disk WAV, not replaying text into an
unknown foreground application. As with earlier milestones, JVM tests do not
prove OEM-specific Accessibility behavior.
