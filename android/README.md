# OpenWispr — Android

A Wispr Flow-style voice tool with a text-rewrite flow, native on
Android. **Tap the floating bubble → speak → the cleaned-up / rewritten text is
typed straight into the field you're in.** Or select text in any app → tap
**Rewrite** in the selection toolbar → pick an action → the rewrite replaces the
selection in place.

This Android fork performs **100% of inference on-device**. Models can be
downloaded online, then speech recognition, text cleanup, local rewriting,
personalization and insertion work offline with no cloud APIs, API keys or
subscription. See [LOCAL_ONLY.md](LOCAL_ONLY.md) for the enforced boundaries
and device acceptance tests.

## How it works

- **Voice flow** (bubble → `RewriteActivity` voice sheet): microphone capture
  (`AudioRecorder`) and local Silero VAD feed `OnDeviceStt`. The user can
  dictate fresh text or speak instructions to rewrite clipboard text. Rules
  and optional local llama.cpp cleanup produce text for insertion into the
  focused field through Android's accessibility APIs, with clipboard fallback.
- **`PROCESS_TEXT` intent** (`RewriteActivity`) — Android shows "Rewrite" in the
  text-selection toolbar in every app. The activity returns the rewritten text
  via `EXTRA_PROCESS_TEXT`, replacing the selection in editable fields.
- **Floating bubble** (`BubbleService`) — always-on draggable overlay button
  (foreground service + "Display over other apps"). Tapping it opens the voice
  sheet.
- **`SettingsActivity`** manages local STT/LLM model selection and downloads,
  language, voice profile, cleanup, accessibility and bubble preferences.
- **`RewriteEngine`** provides prompts and output-validation logic used by
  `LocalLlmEngine` (llama.cpp). Its obsolete remote inference entry points
  reject execution.
- **`OnDeviceStt`** selects local `LocalParakeetStt` (Sherpa-ONNX,
  English Parakeet v2 or optional multilingual Parakeet v3 preview) or
  `LocalWhisperStt` (multilingual whisper.cpp).
  Audio capture is 16-kHz mono PCM. Model weights are downloaded into app
  storage; they are never sent to external inference providers.
- **`ModelDownloader`** is the only type of application component that needs
  network access for provisioning STT/LLM weights.
- **Personalization** includes an on-device vocabulary and correction history.

See [PARAKEET_V3.md](PARAKEET_V3.md) for pinned v3 model provenance, setup
and Android compatibility checks. V3 is not the default model.

For repeatable on-device Whisper/Parakeet quality and latency measurements,
see [BENCHMARK.md](BENCHMARK.md). No benchmark audio is stored in Git.

Conservative offline punctuation and capitalization cleanup for manually selected
Portuguese and Spanish is documented in [MULTILINGUAL_CLEANUP.md](MULTILINGUAL_CLEANUP.md).
English-trained LLM cleanup is skipped for non-English dictation.

Automatic language detection is available as an **opt-in** choice in Settings.
Parakeet v3 and multilingual Whisper detect languages on-device; see
[AUTO_LANGUAGE.md](AUTO_LANGUAGE.md) for routing and code-switching limits.

Native inference lifetime hardening and Android device stress-test gates are
outlined in [NATIVE_LIFECYCLE.md](NATIVE_LIFECYCLE.md).

A future Qwen3-ASR 0.6B engine is subject to the measurement-based decision
in [QWEN3_ASR_GATE.md](QWEN3_ASR_GATE.md); no new inference engine is enabled.

Settings now shows whether the effective speech model is installed for your selected
language and whether optional polish is available; see
[OFFLINE_READINESS.md](OFFLINE_READINESS.md).

The bubble's hold-to-talk gesture can now be disabled or adjusted (300–800 ms) in
Settings; see [BUBBLE_CONTROLS.md](BUBBLE_CONTROLS.md) for behavior and testing.

The floating bubble and dictation sheet distinguish local transcription from
optional polish; see [DICTATION_FEEDBACK.md](DICTATION_FEEDBACK.md).

Cross-app insertion now checks the original recording host and protects Unicode
cursor offsets; see [INSERTION_SAFETY.md](INSERTION_SAFETY.md) for safety and
regression coverage.

A queued accessibility insert is no longer treated as delivered until Android
confirms insertion or clipboard fallback. See
[DELIVERY_ACKNOWLEDGEMENT.md](DELIVERY_ACKNOWLEDGEMENT.md).

## Build

For the fork's P0.0 baseline, Android CI validation and on-device smoke tests,
see [BASELINE.md](BASELINE.md). Both Android CI and release builds fetch the pinned
sherpa-onnx AAR through `scripts/fetch-sherpa-aar.sh`, which validates its
published SHA-256 before Gradle runs.


Requires JDK 17 (Android Studio bundles it) and the Android SDK. The on-device
Whisper module (`:lib`) compiles native code, so you also need the **NDK** and
**CMake** (install via Android Studio → SDK Manager → SDK Tools). The native build
targets **arm64-v8a** only (see `lib/build.gradle.kts` `abiFilters`).

```bash
cd android
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

Or open the `android/` folder in Android Studio and Run.

The `:lib` module vendors a pinned copy of whisper.cpp (v1.7.4, CPU-only ggml
backend) under `lib/src/main/jni/whispercpp/`. The `:llm` module vendors
llama.cpp + ARM's `aichat` JNI under `llm/src/main/cpp/`, built with **NDK 29 +
CMake 3.31** (install both via SDK Manager) for arm64-v8a, CPU-only.

## Install (sideload, no Play Store)

With the phone connected over USB (USB debugging on):

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Or copy the APK to the phone and tap it (allow "install unknown apps" once).
A debug-signed APK is fine for personal use indefinitely.

## First run

1. Open **OpenWispr** from the launcher.
2. Download an on-device speech model (Parakeet v2 for English, or multilingual
   Whisper for PT-PT, Spanish and other languages). The installed weights run
   offline thereafter.
3. For AI polish, download an optional local Qwen/Gemma GGUF model. Without a
   local LLM model, deterministic cleanup remains available.
4. Enable auto-insert under Android Accessibility, and grant microphone and
   overlay permissions as required. Without auto-insert, use clipboard fallback.
5. Tap the floating bubble to dictate, or select text in another app and choose
   **Rewrite** to process it using the on-device LLM.
6. Disable Wi-Fi and mobile data to verify local inference after provisioning.

## Floating bubble

Settings → toggle **Floating bubble** on. The first time it asks for "Display
over other apps" — allow it. A blue bubble appears; drag it anywhere.

Gestures:
- **Single tap** → starts dictation immediately. The bubble turns red and shows a
  live **waveform** while you speak. **Tap again** (or tap anywhere) to stop; the
  rewrite is transcribed, cleaned up, and you tap **Insert**.
- **Long-press** → starts a **Rewrite clipboard** recording (speak a command like
  "make this formal" to rewrite whatever you copied).
- **Drag** → reposition the bubble. Drag it onto the **✕ target** at the bottom
  center to dismiss it (re-enable later from Settings → Floating bubble).

With accessibility on, the result types into your focused field; otherwise it's
copied to paste.

The bubble can be restored after reboot or an app update when the floating-bubble preference was enabled. Android/OEM background-start restrictions may prevent this on some devices.

For starting or stopping the bubble, use **Settings → Floating bubble**. External ADB broadcasts to `BubbleControlReceiver` are no longer supported: the receiver is private to the application to prevent another app from starting or stopping the overlay.

## Notes / limits

- In-place replace works in editable fields. Some apps restrict the action or
  mark selections read-only; there you get clipboard copy.
- The six prompts are the built-in defaults (not yet editable in the UI —
  edit `Defaults.DEFAULT_PROMPTS` to change them).
- **No external inference:** Cloud STT/LLM provider options have been removed
  from Settings. Previous cloud-provider selections are reset to local models
  and obsolete API credentials are discarded (including old encrypted keys).
  Model downloads still require internet connectivity.
- Existing installations may need to download a different on-device model after
  upgrading. Model file availability is checked before inference.
- Debug logs record model metadata and lengths rather than text content.
- Automatic Android cloud backup and device-to-device transfer are explicitly excluded for this fork, to avoid exporting API keys, dictation history, audio recordings, and personal vocabulary. **Uninstalling the application can therefore permanently delete local data.**
