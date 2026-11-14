# P3 — Accessibility insertion safety and regression coverage

The on-device dictation pipeline captures the host application's package
before the microphone starts. Processing can take several seconds, and the
foreground app or focused field can change during that time.

## Safeguards

- When a known recording host exists, accessibility insertion accepts an
  editable field **only in that application**, rather than writing text into
  another app that becomes foreground during transcription or optional polish.
- The service checks both the active window's package and the focused editable
  node's package. Its own window, system surfaces and keyboard packages cannot
  receive dictated text.
- If the host app cannot be found within the existing retry window, the normal
  **clipboard fallback** is used; the bubble reports *Copied*, not *Inserted*.
- The field text-splice plan is a pure function. It respects selection bounds,
  reversed selections, insertion offsets, UTF-16 surrogate-pair boundaries,
  and preserves text outside the selection. Invalid cursor information in a
  nonempty field falls back to the existing paste behavior.
- If there is no known original host (for instance a manual retry from Home),
  the historical non-system editable-field fallback remains.
- The original clipboard content is not touched on a successful
  `ACTION_SET_TEXT` splice.

No cloud inference, internet requests, new permissions, model changes or
changes to speech-language routing are introduced.

## JVM regression suite

`InsertionTextPlannerTest` validates insertion at start/middle/end, selecting
and replacing words, reversed ranges, PT-PT/Spanish accents, newlines, emojis,
surrogate boundaries and invalid cursor offsets.

`InsertionTargetPolicyTest` validates original-host affinity, app switching,
rejection of system/keyboard/self windows, missing focus metadata, and
unidentified-host fallback.

## Device acceptance

- [ ] Android CI: debug APK and instrumentation APK compile; JVM tests pass.
- [ ] Dictate in App A, switch to App B before inference completes: text must
      **never** be inserted into App B. After retry timeout it is copied to
      clipboard, with the correct bubble status.
- [ ] Dictate in App A, briefly switch away and back before retry timeout:
      insertion may succeed only if App A's editor is focused again.
- [ ] Select a word within a message and dictate its replacement; surrounding
      text, accents, emoji and newlines must remain intact.
- [ ] Repeat with cursor at start/middle/end, including different IMEs and
      WebView/Compose fields.
- [ ] Confirm successful direct insertion leaves clipboard untouched; fallback
      may overwrite clipboard by design and shows Copied.
- [ ] Verify repeated dictations, cancellation and retry remain functional
      with Wi-Fi/mobile data disabled.

**Remaining limitation:** package affinity cannot distinguish between two
different text fields inside the *same* host app. Detecting a stable field
identity would require additional instrumentation and real-device testing.
The above JVM tests do not prove behavior across Android vendor accessibility
implementations; device testing is a separate gate.
