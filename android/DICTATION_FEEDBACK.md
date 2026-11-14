# P3 — Distinct transcription and polish feedback

The Android floating bubble and the visible dictation sheet now distinguish
two local processing stages after recording finishes:

- **Transcribe** — recorded speech is being decoded by the selected on-device
  Whisper or Parakeet model.
- **Polishing** — optional local text cleanup is processing the transcript.
  This stage is shown only when the cleanup pipeline actually starts.

The compact bubble keeps its existing processing animation while displaying a
short stage caption. Accessible descriptions explain the work without showing
the dictated words. Successful text insertion and clipboard fallback continue
to have separate states: **Inserted** means a field write succeeded, while
**Copied** means the text was placed on the clipboard for manual pasting.

There are no artificial percentages, new telemetry calls, cloud dependencies,
additional permissions, model changes, or altered dictation/insertion logic.

## Validation

- [ ] Android CI: assemble debug APK, instrumentation APK and JVM tests.
- [ ] Run local transcription with a model already installed and with networking
      disabled. Confirm transcription feedback appears after recording ends.
- [ ] With local polish enabled and available, confirm the status changes to
      Polishing only when optional cleanup actually begins.
- [ ] With polish disabled, or non-English/Automatic dictation, verify the
      Polishing status is never shown.
- [ ] Verify accurate accessibility descriptions for idle, recording,
      transcription, polish, inserted and clipboard fallback states.
- [ ] Verify tapping, holding, dragging, cancelling and insertion are unchanged.
- [ ] Verify no transcription content is placed in bubble accessibility text.
- [ ] Check dark mode, display scaling and TalkBack on Android 17.

JVM tests check the mapping and invariants. They do not replace device-level
UI/accessibility acceptance.
