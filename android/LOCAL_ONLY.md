# P0.1 — Local-only Android inference policy

**Model downloads may use the internet.** Audio, transcript text, prompts and
personalization data are processed locally, on the Android device; installed
models must remain usable with networking disabled. This is an inference
restriction, **not** an air-gapped provisioning requirement.

## Enforced boundaries

- `LocalOnlyPolicy.enforce()` normalizes legacy cloud configurations on
  SettingsRepository reads and writes. Cloud provider IDs, model IDs, keys,
  and endpoints cannot be selected or persisted as active inference settings.
- `Defaults.PROVIDERS` and `Defaults.STT_PROVIDERS` expose only `local`.
  The Android Settings and onboarding flows expose only the local engines.
- `RewriteActivity` calls `OnDeviceStt.transcribe()` for recognition and
  `LocalLlmEngine.streamWithPrompt()` for optional rewriting/cleanup.
- The old `SttEngine` and `RewriteEngine.streamWithPrompt()` remote entry
  points fail closed and contain no HTTP network inference implementation.
- The Android app retains `INTERNET` permission, but only the model managers
  use it to download the required Whisper, Parakeet and local GGUF weights.
  No remote ASR/LLM keys or endpoints are offered.
- Obsolete API keys are removed from the encrypted credential vault and
  legacy plaintext DataStore entries. Upgrading intentionally retires cloud
  inference; do **not** rely on this build to retain API keys for a future fork.
- Local LLM logs record only model metadata and character counts, never
  dictated or generated text.

## Acceptance test

1. Verify `assembleDebug` and `testDebugUnitTest` pass on CI.
2. In Settings, confirm the only supported providers are on-device models.
3. Migrate an installation whose previous settings selected cloud STT and/or
   LLM. Confirm local model IDs are selected and no cloud endpoint is called.
4. With an internet connection, download a Whisper multilingual model and
   an on-device cleanup GGUF, then test a normal dictation.
5. Disable Wi-Fi and mobile data. Transcribe and clean 10 short and 10 long
   recordings, including PT-PT, Spanish and English; verify cross-app insertion.
6. Repeat after reboot and a fresh launch without network connectivity.
7. Capture traffic during inference: no remote transcription, LLM inference,
   prompts, audio or text should leave the device. Model provisioning is an
   explicitly separate, online step.
8. Ensure local model missing/errors produce a download prompt rather than
   cloud fallback.

## Out of scope

This change does not add Parakeet v3, automatic language detection, language
quality tuning or redesigned storage. Those are subsequent roadmap milestones.

**Status:** Source implementation requires CI + device validation before P0.1
can be marked complete.
