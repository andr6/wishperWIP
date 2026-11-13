# Android engineering baseline — P0.0

The target is **local on-device inference**. Downloads for model provisioning are permitted,
but transcription, text cleanup, personalization and insertion must continue working
without internet access once models have been installed. This milestone validates the
existing implementation; strict local-only provider enforcement is a separate P0.1 task.

## Source and toolchain

- Fork: `andr6/wishperWIP`; default branch: `main`.
- Android: Kotlin 2.0.20, AGP 8.9.3, Gradle wrapper 8.11.1, JDK 17.
- Android compile/target SDK 36; minimum SDK 24; arm64-v8a only.
- Native dependencies: Android NDK 25.1.8937393 + 29.0.13113456,
  CMake 3.22.1 + 3.31.6.
- Sherpa-ONNX AAR: v1.13.3, downloaded by `scripts/fetch-sherpa-aar.sh`
  and checked against SHA-256 `9f065fe6f2cab09fd48eaa580097293e077637ad53a5e89c5c58a36509386ac7` as published for the upstream GitHub release asset.
- Inference/model files are downloaded separately inside the running application.
  Model weights are not vendored into the source checkout.

## Repeatable developer build

Prerequisites: JDK 17, Android SDK platform 36, the two listed NDK releases and
CMake releases; Android SDK licenses accepted.

```bash
cd android
bash scripts/fetch-sherpa-aar.sh
./gradlew :app:assembleDebug :app:testDebugUnitTest --no-daemon --stacktrace
```

Expected APK: `android/app/build/outputs/apk/debug/app-debug.apk`.
Expected JVM test reports: `android/app/build/reports/tests/testDebugUnitTest/`.
This does **not** prove JNI runtime loading, speech quality, or Android 17 compatibility.

## GitHub Actions setup on a fork

GitHub often disables Actions workflows in new forks until the owner explicitly enables
them. Open the fork's **Actions** tab, review the workflow, and enable Actions if prompted.
Run the `CI` workflow on pull requests (or select **Run workflow** from `main` once the
workflow_dispatch change is merged). Manual dispatch skips macOS and tests Android only;
normal pushes/PRs retain the existing macOS job. Android artifacts contain the unit-test
results, plus the debug APK on a successful build.

Do not configure upstream Play publishing or reuse upstream Android signing secrets for
this fork. The existing `release.yml` refers to upstream publishing secrets and should
not be triggered just to validate a debug build.

## Device acceptance: OnePlus CPH2747 / Android 17

Use a developer/debug install separately from important production data. The existing
application ID is `com.voicerewriter`; a debug build can replace an installed build only
if the signing certificate matches. An incompatible existing installation should **not**
be uninstalled before data/export considerations are understood.

1. Record OS build, available RAM/storage and the selected local STT + LLM models.
2. Launch app and grant microphone/accessibility/overlay permissions deliberately.
3. Download models online; verify model-manager readiness and complete first inference.
4. Disable Wi-Fi and mobile data; repeat 10 short and 10 long dictations. Confirm all
   transcript processing and text insertion still complete with the device offline.
5. Validate insertion in at least two editable hosts (e.g. a plain text editor and chat
   composer), including cursor/selection replacement and clipboard fallback behavior.
6. Check recording stop/retry/cancel, overlay visibility, language selection and reboot
   recovery; observe process crashes, ANRs and native model loading.
7. On a test install with pre-existing cloud API credentials, separately verify the
   merged PR #2 encrypted-credential migration. That is a compatibility test, **not**
   permission to send dictations to cloud APIs in our future local-only build.

## Baseline evidence to attach

- GitHub Actions run URL and commit SHA; green `assembleDebug` + `testDebugUnitTest`.
- Full Gradle output, JUnit results and SHA-256-verified AAR record.
- Device OS/build, memory, STT/LLM model IDs, and free storage.
- Samples and correctness annotations for English, European Portuguese and Spanish
  (with private audio retained only in the test environment).
- Timing: model cold/warm load, recording-stop-to-insertion latency (p50/p95),
  transcription duration, peak resident memory, and failures per 20 dictations.

**Gate:** P0.0 is complete only after the Android CI build passes and device smoke tests
succeed. Source changes alone do not satisfy this gate.
