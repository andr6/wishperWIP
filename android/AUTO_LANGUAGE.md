# P1.1 — Automatic language detection (Android)

All speech detection and transcription are performed **on-device**. The internet
is used only for downloading the chosen speech models. After the weights are
installed, automatic language recognition works without network access.

## What this change supports

- Settings → Voice → Dictation language → **Automatic (detect speech)**.
- The setting is **opt-in**: existing installs and newly provisioned phones keep
  their historical English default until the user selects Automatic.
- **Parakeet v3**: the multilingual model internally recognizes the spoken
  language among its 25 supported European languages. It supports automatic
  input; the application does not force a language selection during inference.
- **Whisper multilingual**: passes `auto` to whisper.cpp's built-in language
  detection in the existing JNI transcription call.
- **Parakeet v2**: English only; requesting Automatic changes the active engine
  to an available multilingual Whisper model instead of falsely claiming
  automatic support.
- **Whisper Hinglish fine-tunes**: fixed decode languages cannot auto-detect;
  requesting Automatic routes to generic multilingual Whisper.
- Manual PT, ES, EN and other supported selections still work. V2/v3 routing
  remains language-aware.
- **Safe postprocessing:** Automatic is NOT treated as English; existing
  English-specific deterministic cleanup is skipped instead of corrupting
  potentially Portuguese or Spanish transcripts. Later multilingual cleanup
  will use reliable detection metadata to choose language-specific rules.

## Known limitation: language switching *within* a recording

Automatic recognition primarily identifies a language **for each recording**.
Neither Whisper nor Parakeet v3 should be assumed to transcribe every
English-Portuguese or English-Spanish code switch correctly within a single
sentence. Short clips, accents, acronyms and embedded English technology terms
can alter language identification and transcription accuracy.

No cloud fallback, translation service, or secondary inference engine is used
for detecting the language. We retain a manual language override for cases
where Automatic performs poorly.

## Acceptance on the OnePlus / Android 17

1. Download v3 and multilingual Whisper models while online.
2. Select Automatic, then disable Wi-Fi and mobile data.
3. Record 10 PT-PT, 10 Spanish and 10 English samples. Check that the text
   remains in the spoken language (no translation).
4. Record 10 mixed PT-PT/English, and 10 mixed Spanish/English sentences.
   Include IT names and acronyms, such as `firewall`, `endpoint`, and `SIEM`.
5. Toggle between Automatic and explicit PT/ES/EN and repeat the same samples.
6. Verify the Settings engine selector does not leave an English-only model
   active when Automatic is chosen.
7. Verify missing model errors do not trigger cloud inference or translation.
8. Confirm English-only cleanup does not alter automatic PT/ES results.

The device benchmark accepts fixtures tagged `auto` in `fixtures.tsv`
with human-verified reference transcripts containing both languages:

```text
mix-pt-en-01	auto	Vamos testar o endpoint antes de fazer deploy.
mix-es-en-01	auto	Revisamos el firewall y después hacemos el deployment.
```

Use real 16-kHz PCM16 WAV recordings with those same IDs, and run the benchmark
on v3 and Whisper (not Parakeet v2). Read the language-specific records from
`results.jsonl`, not just the aggregate average. A mixed-language corpus is
a *test of performance*, not a guarantee of seamless code-switching.

**Exit gate:** Android CI green, real on-device automatic recognition working
offline, mixed-language comparisons captured and reviewed, and no silent
fallback to English-only inference.
