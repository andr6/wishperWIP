# P2.0 — Conservative multilingual dictation cleanup

The Android fork supports local-only inference and online model provisioning.
This milestone does not add a new LLM or new network APIs.

## Behavior

- **English (manual `en`):** keeps the existing deterministic cleanup and
  optional local Qwen/Gemma polish logic.
- **European Portuguese (manual `pt`) / Spanish (manual `es`):**
  applies narrow punctuation-spacing and sentence-initial capitalization rules
  to the raw ASR output. Keeps words, accents, numbers, proper nouns,
  abbreviations, acronyms and technical terms. Existing paragraph breaks are
  preserved. No translation or word substitutions.
- **Automatic language (`auto`):** preserves the ASR result unchanged for now.
  Model language identification metadata is not yet threaded to cleanup; do
  not silently assume English from a Latin-script transcript.
- **Other languages and Hinglish transliteration:** unchanged.
- **Code/terminal contexts:** multilingual deterministic formatting is
  bypassed to avoid changing syntax, paths or values.
- **English-trained local Qwen/Gemma polish:** skipped for **all non-English
  and Automatic** dictation, because those models have not been evaluated for
  preserving PT-PT, Spanish or mixed-language content. English-only rewriting
  of selected text is separate from this dictation-cleanup gate.

## Unit-test coverage

- PT-PT and Spanish punctuation spacing, accents and sentence starts.
- Spanish inverted punctuation, Portuguese diacritics and decimal numbers.
- Technical terms, URLs, email addresses, capitalization inside product names.
- Paragraph breaks, deterministic/idempotent output.
- Automatic and unsupported languages are left untouched.
- Code input bypasses the formatter.
- English-trained LLM polish is permitted only in explicitly selected English.

## Validation on OnePlus / Android 17

1. Download a multilingual Whisper model or Parakeet v3 while online.
2. Switch Wi-Fi/mobile data off. Select Portuguese manually, dictate at least
   10 typical PT-PT recordings, including Portuguese accents, dates, email
   addresses, CVE IDs, company names and technical terminology.
3. Repeat in Spanish and English. Explicit English should retain previous
   cleanup behavior.
4. Switch to Automatic and test 10 PT-PT/English and 10 Spanish/English mixed
   clips. The raw ASR content should not undergo English-only cleanup or
   Qwen polishing.
5. Use a code editor/terminal field. Verify exact syntax and identifiers are
   not modified by multilingual cleanup.
6. Confirm that no network inference requests are made and that transcription
   and insertion work offline.
7. Check no material content loss; record observed differences against a
   human-verified transcript.

## Limitations and next tasks

This is **conservative formatting**, not full PT-PT/Spanish grammar repair.
Model-specific languages or locale variants (e.g. PT-PT vs PT-BR) still need
reliable detected-language signals and explicit evaluation. The v3 model and
Qwen/Gemma language quality need real-device benchmarking before changing
defaults or expanding model-driven rewriting.

**Merge gate:** Android CI green; device-level multilingual cleanup checks
still require a real Android phone and should not be represented as proven
by JVM tests.
