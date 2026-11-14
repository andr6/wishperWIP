# P1.0 — reproducible Android ASR benchmark

**Goal:** compare existing on-device models using identical audio. Internet is
allowed to download model weights, but inference never sends audio or text off
the phone. This benchmark runs directly through `OnDeviceStt`; it does **not**
measure VAD, text cleanup, LLM, bubble UI, or cross-app insertion. Evaluate
those separately as end-to-end latency/quality tests.

## Tested engines

- `parakeet`: Parakeet v2, **English only**. Non-English recordings are
  intentionally skipped rather than silently routed through Whisper.
- `parakeet-v3`: Parakeet v3, English/Portuguese/Spanish and supported
  European languages. Device/JNI compatibility must be demonstrated.
- `tiny`, `base`, `small`: Whisper multilingual models. Do not compare an
  English-only model on languages it cannot recognize.

## Prepare a private corpus

Prepare 16-kHz, mono PCM16 WAV files with a standard **44-byte header**, and a
tab-separated `fixtures.tsv` with three columns per line:

```text
# id<TAB>language<TAB>human-verified reference transcript
pt001	pt	Hoje quero testar o reconhecimento de voz em português europeu.
es001	es	Mañana tendremos una reunión con el equipo de seguridad.
en001	en	I'll send the report after lunch.
```

Each row must have a matching `pt001.wav`, etc. in the same directory.
Use **real recorded speech**, not synthetic text, and keep accents in the
verified reference. Record short/long phrases, disfluencies, names/acronyms,
noise and mixed-language cases. Start with at least 30 PT-PT, 30 Spanish,
30 English and 15 mixed-language clips (code-switching needs its own
annotations and benchmark pass). WER/CER are case- and punctuation-insensitive
but **accent-sensitive**.

Do not check any personal audio, transcript, or `results.jsonl` into Git.
The app data lives under `filesDir/benchmark` and is excluded from Android
backup by the project's backup policy.

## Build and run on an Android ARM64 device

Pre-download each model through the app's **Settings → Voice** UI while online.
Then disconnect Wi-Fi/mobile data to verify true offline inference.

```bash
cd android
bash scripts/fetch-sherpa-aar.sh
./gradlew :app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest --no-daemon

adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk

# After app first launch, copy the private fixture directory.
adb push ./benchmark-fixtures/. /sdcard/Android/data/com.voicerewriter/files/benchmark/

adb shell am instrument -w \
  -e class com.voicerewriter.OnDeviceBenchmarkTest \
  -e models parakeet,parakeet-v3,base \
  -e repeats 2 \
  com.voicerewriter.test/androidx.test.runner.AndroidJUnitRunner

adb pull /sdcard/Android/data/com.voicerewriter/files/benchmark/results.jsonl ./results.jsonl
adb pull /sdcard/Android/data/com.voicerewriter/files/benchmark/summary.json ./summary.json
```

If `adb push` to Android/data is blocked by the device, use Android Studio's
Device Explorer to upload the files to the application's
`/data/data/com.voicerewriter/files/benchmark/` directory while debugging.

Run on the device rather than CI emulators: this is ARM64 native inference
and depends on downloaded on-device weights. CI **compiles** the instrumentation
APK and runs pure JVM metrics tests, but cannot claim ASR quality results.

## Results and interpretation

The instrumented test writes **one JSONL record per model/clip/repetition**,
including model ID, language, run number, latency, real-time factor, WER, CER,
empty-output flag, inferred error, and PSS/native heap before/after snapshots.
A machine-readable `summary.json` contains latency p50/p95, failure count,
empty count, mean WER/CER by model.

- First call in a session may include cold model load. Subsequent calls
  measure warm behavior. Compare like with like; do not mix these blindly.
- PSS/native heap are **before/after snapshots, not true peak RAM**.
- The main result is **ASR-only latency**; end-to-end
  record-stop-to-field-insertion latency is a separate measurement.
- WER/CER are not by themselves sufficient for code-switching, proper-name
  retention, or European Portuguese style differences.
- Failed model loads count as failures; unavailable models cause the benchmark
  to fail rather than being silently substituted.
- No speech data, transcripts, or API calls leave the phone; the output files
  remain local unless explicitly transferred via USB.

**Exit criteria:** green Android compilation/unit tests; v3 loads on Android 17;
all applicable models run offline; repeatable device measurements exist for
accuracy, latency, memory snapshots and failures; then decide the default.
