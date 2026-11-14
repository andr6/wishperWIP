# P3 — Configurable bubble controls

The existing Android floating bubble supports tap-to-toggle dictation, press-and-hold
to dictate until release, and drag-to-reposition/dismiss. This change keeps those
gestures while allowing the hold shortcut to be adjusted.

## Settings → Bubble

- **Hold to talk**: enabled by default, preserving current behavior. When off,
  holding the bubble does not launch a push-to-talk recording; releasing still
  acts like the existing tap if the bubble was not dragged.
- **Hold activation time**: from 300 to 800 milliseconds, in 50-ms increments.
  Default is the original 450 ms. The range is bounded and normalized on
  both preference reads and writes.
- **Tap**: always starts or stops recording as before. The active recording
  stopper takes priority, preventing a long hold from starting another take.
- **Drag**: movement past the drag threshold cancels the long-press timer.
  Release after dragging still docks the bubble; dismissing on the target
  still disables the service.

Settings persist in the existing private DataStore. The running bubble
reloads them when Settings persists changes, so an app restart is not required.
No new permissions, cloud endpoints, subscriptions, or network calls are added.

## Regression coverage

`BubbleGestureConfigTest` checks preserved defaults, bounds and rounding,
disabled shortcut, dragging and already-active recording safety.

## Android device acceptance

- [ ] Android CI assembles the APK and instrumentation APK and passes JVM tests.
- [ ] Toggle the shortcut off; verify tap starts/stops and hold does not start
  push-to-talk; drag/dismiss still work.
- [ ] Enable shortcut at 300 ms, 450 ms and 800 ms; verify the expected timing.
- [ ] Start a tap-to-toggle recording, then hold the bubble: it must stop the
  existing recording, never start a second take.
- [ ] Begin holding then drag before activation; no unintended recording.
- [ ] Test immediate release after a hold fires, including when the recording
  activity has not yet initialized.
- [ ] Change the setting with the service already running and after restart.
- [ ] Perform dictation offline after downloading speech models; validate text
  insertion and no regressions in Android 17 gesture handling.

This milestone configures a UI shortcut. It does not change ASR model quality,
language handling or the offline inference boundary.
