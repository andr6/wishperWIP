# P3 — Offline readiness in Android Settings

The app can download model files from the internet. Recognition, cleanup and
text insertion remain local and can work without networking after installation.

## What Settings now reports

- Whether the **effective** speech engine selected for the current language is
  installed. If language routing chooses a downloaded multilingual Whisper
  fallback instead of an incompatible Parakeet v2 configuration, the displayed
  status reflects that actual fallback, not the obsolete selection.
- A **Download speech model** action when the needed engine is absent. Downloads
  use the existing model managers. No inference or automatic download is
  started by checking readiness.
- Optional local polish availability. Missing cleanup weights do **not**
  prevent dictation. The optional download action is offered only when polish
  is enabled and language is explicitly English, matching the current
  conservative non-English dictation policy.
- Model changes and successful downloads refresh the local readiness panel.
- Readiness does **not** imply that Android permissions, native model load,
  microphone capture or cross-app text insertion have been validated.

## Important distinctions

- **Speech ready** means the currently routed local STT model reports a
  downloaded bundle. It does not prove actual ONNX/Whisper JNI compatibility.
- **Optional polish ready** means its GGUF file passes the current model
  manager's local readiness check; generation is not performed in Settings.
- **Automatic**, European Portuguese and Spanish dictation must not be
  described as using the English-trained cleanup model.
- No cloud inference APIs, subscriptions, speech uploads, or external
  diagnostic calls have been added.

## Acceptance criteria

- [ ] Android CI debug build, instrumentation APK and unit tests pass.
- [ ] Missing STT model produces a clear status and appropriate download
  action, not a claim of offline readiness.
- [ ] Installed multilingual Whisper selected as a language fallback is
  reported accurately, including when Parakeet v2 is selected for PT-PT.
- [ ] Missing optional English polish does not block dictation.
- [ ] PT-PT, Spanish and Automatic show the polish limitation without prompting
  for an irrelevant English cleanup model.
- [ ] UI refreshes after model download or deletion and after changing
  language/model.
- [ ] Offline device test verifies recognition and insertion after
  provisioning while both Wi-Fi and mobile data are disabled.

This is a usability and configuration visibility change. It is not a
substitute for the real-device benchmarking and stability gates already
tracked in the project.
