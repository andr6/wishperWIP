# P3 — Qwen3-ASR 0.6B decision gate (no new engine yet)

Qwen3-ASR is a possible **local-only** recognition engine; adding it is
conditional on measured benefits over existing models. Model files may be
downloaded online and verified; inference must remain completely offline.

## Feasibility evidence

- The existing sherpa-onnx **v1.13.3** Android binding already declares
  `OfflineQwen3AsrModelConfig`, with `convFrontend`, `encoder`,
  `decoder`, and `tokenizer` fields. Thus the Java/Kotlin binding has a
  plausible integration route without automatically replacing the AAR.
- A sherpa-compatible Qwen3-ASR 0.6B INT8 bundle is distributed separately:
  https://huggingface.co/csukuangfj2/sherpa-onnx-qwen3-asr-0.6B-int8-2026-03-25
- Presence of a binding or model files does **not** prove compatibility with
  the exact AAR used in our Android APK, memory fit on the phone, or improved
  PT-PT / Spanish accuracy.

## Required comparison before selecting Qwen3-ASR

Run the existing benchmark **on the target device**, offline, with the same
private human-transcribed audio across supported candidates. See
[BENCHMARK.md](BENCHMARK.md). Evaluate at minimum:

- 30 European Portuguese, 30 Spanish and 30 English recordings; add at
  least 15 PT-PT/EN and ES/EN code-switching clips
- WER, CER, hallucination/blank output and retention of acronyms, proper
  names, decimal values and CVE IDs
- Cold- and warm-start decoding latency (p50/p95), real-time factor,
  before/after PSS and native heap
- Real-device native model load/crash behavior, repeated switches, thermal
  throttling, and battery usage measured with a consistent workload

Treat model load failure and out-of-memory as disqualifying until resolved.

## Gated implementation, only if warranted

1. Pin an immutable upstream revision and **verified SHA-256 and byte size**
   for each file, with a separate model directory, safe/resumable downloads.
2. Build a **preview** using the existing sherpa-onnx `OfflineQwen3AsrModelConfig`.
   Keep Whisper and Parakeet v2/v3 available; do not change defaults.
3. Confirm the Android ARM64 AAR loads the model, transcribes three target
   languages fully offline, and handles cancellation/model switching.
4. Extend the benchmark with its model ID. Compare identical device results;
   do not substitute another model under the candidate's label.
5. Consider enabling as a default **only** if objective gains outweigh disk,
   peak RAM, p95 latency, power use and integration complexity.

**Status:** feasibility acknowledged; **no Qwen3-ASR weights, loader or native
inference path are included in this PR**. Device benchmark results have not
been supplied. A decision to skip P3 is valid if Parakeet v3 and Whisper
already meet accuracy/latency requirements.
