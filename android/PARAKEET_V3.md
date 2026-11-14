# P0.2 — Optional Parakeet TDT 0.6B v3 preview on Android

Parakeet v3 is an experimental **on-device** speech-recognition model.
Its NVIDIA release supports 25 European languages and automatic language
identification. It is not automatically selected and does not replace
Parakeet v2 (English only) or multilingual Whisper. Inference never calls a
cloud API; the model bundle can be downloaded over the internet.

## Provenance and repeatability

Source: `csukuangfj/sherpa-onnx-nemo-parakeet-tdt-0.6b-v3-int8` on Hugging Face,
revision `876ff91b4ab4b89c328afdb2b27ff879d3e42f87`.
The exact ONNX weights + tokenizer are declared in
`ParakeetV3ModelManager.ASSETS`, with pinned byte counts and SHA-256 digests.

Required files:

| File | Expected bytes |
| --- | ---: |
| encoder.int8.onnx | 652,184,281 |
| decoder.int8.onnx | 11,845,275 |
| joiner.int8.onnx | 6,355,277 |
| tokens.txt | 93,939 |

All files reside in `filesDir/models/parakeet-v3/`, separately from the v2
bundle in `filesDir/models/parakeet/`. Downloads resume after interruption,
and a bundle is marked ready only after every checksum passes. A local
verification marker is written only when all four files match.

## Use

1. Settings → Voice → select **Parakeet v3 · multilingual (preview)** and
   download it (approximately 671 MB of model data; allow ample free storage).
2. Select English, Portuguese, Spanish, or another supported European language.
   Unlike v2, the v3 model handles those directly and automatically identifies
   speech language. The application still exposes a selected-language preference.
3. Record a short utterance, then check the result against an accurate transcript.
4. Disable network connectivity and repeat transcription, cleanup, and text insertion.

V3 uses Sherpa-ONNX NeMo transducer `greedy_search`; the v2 hotwords BPE
vocabulary and experimental modified beam decoder are deliberately disabled
for v3. The native recognizer is rebuilt and the previous instance released
when switching between v2 and v3, with decode serialized to prevent
use-after-release.

## Compatibility / safety gates

- [ ] Android CI debug APK and unit tests pass.
- [ ] Model downloads complete and SHA-256 checks pass on OnePlus CPH2747.
- [ ] Android Sherpa-ONNX v1.13.3 loads the complete v3 INT8 bundle.
- [ ] Transcribe PT-PT, Spanish and English with Wi-Fi/mobile data disabled.
- [ ] V3 → V2 → V3 and V3 → Whisper transitions do not crash or exhaust memory.
- [ ] Unavailable/unsupported language routes to downloaded multilingual Whisper.
- [ ] Compare transcript correctness, cold/warm latency, p95, peak RAM and
      battery use against the existing v2/Whisper baseline.

**Status: preview; no production default change before device acceptance.**

NVIDIA model card: https://huggingface.co/nvidia/parakeet-tdt-0.6b-v3
Sherpa conversion: https://huggingface.co/csukuangfj/sherpa-onnx-nemo-parakeet-tdt-0.6b-v3-int8
