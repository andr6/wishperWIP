# P2.1 — Native inference lifecycle hardening

Android-only, local-only, and backward compatible. Model downloads are allowed
over the internet, but ASR and LLM generation remain completely on-device.

## Problem

The previous Whisper implementation synchronized **only context loading**:
after acquiring a cached `WhisperContext`, a concurrent warm-up/model switch
could free the JNI pointer while `transcribeData()` was still running.
Each loaded Whisper context also owned an executor that was not shut down
during model switching, leaking worker threads.

The local llama.cpp wrapper similarly synchronized its model `cleanUp()`,
`loadModel()`, and system prompt, but released the mutex **before** collecting
generated tokens; a second rewrite could replace its model during streaming.

## Changes

- `ExclusiveModelCache` owns one native Whisper context and holds an exclusive
  coroutine mutex through **load → full transcription → release on switch**.
  Warm-up shares the same guard. This removes use-after-release races while
  allowing concurrent callers to wait without blocking the main/UI thread.
- Whisper context disposal closes its dedicated thread dispatcher. Cleanup
  executes in a `NonCancellable` native-worker context so an interrupted
  operation cannot strand native memory during model switching.
- `LocalLlmEngine` holds its existing mutex through the entire stream, not
  just initialization. Rewrite requests therefore cannot replace another
  request's model while its tokens are still being emitted.
- JVM tests use fake native handles and controlled coroutines to check
  warm-up reuse, switching after in-flight decode, load failure and recovery.

No model defaults, decoders, language settings, weights, or prompts change.

## Acceptance

1. Android CI: `assembleDebug`, `assembleDebugAndroidTest` and
   `testDebugUnitTest` pass.
2. On OnePlus CPH2747 / Android 17, download Whisper, Parakeet v2/v3 and an
   optional local LLM while online.
3. Run 20 offline dictations, including concurrent warm-up/start and
   Whisper model switches. Verify stable transcription and no native crash.
4. Trigger overlapping local rewrite requests and cancellation/retry; verify
   each request uses its own intended prompt/model with no mixed output.
5. Repeat v3 ↔ v2 ↔ Whisper transitions; inspect crashes, ANRs and native RSS.
6. Collect model cold/warm load time, stop-to-insertion p50/p95, before/after
   PSS, and battery impact. No performance or battery improvement is claimed
   without measurements.
7. Test background/foreground, screen rotation and notification/bubble service
   restarts; verify no orphan JNI workers after model switching.
8. Confirm model downloads are allowed and inference continues offline.

**Limitations:** Code-level synchronization and unit tests do not prove native
JNI compatibility or background behavior on a real device. Instrumented
lifecycle/stress tests and device profiling remain required before closing
P2.1 for production.
