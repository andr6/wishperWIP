# P3 — Atomic pending-audio metadata commits

## Problem

The current pending-audio store protects the WAV using a temporary file,
but previously wrote its `.json` sidecar directly using `File.writeText`.
A disk-full error, cancellation or process interruption could therefore
truncate or partially overwrite the sole metadata copy. Such a recording
would disappear from Home's retry list even though its audio still existed.

## Changes

- `AtomicRecordingMetadata` writes new JSON into a uniquely named temporary
  file in the same directory, flushes/syncs file contents, then replaces the
  old metadata using a same-directory rename.
- If writing or renaming fails, any temporary file created by this attempt
  is deleted and the previously committed metadata is left untouched.
- `PendingAudio.begin` only adds a recording to its in-flight index after
  the first sidecar commit succeeds. Failed first metadata commits remove
  the corresponding newly written WAV so no unindexed orphan is created.
- `PendingAudio.settle` only applies history retention and pruning after the
  terminal metadata successfully commits. On commit failure, the existing
  WAV and last committed metadata stay in place for recovery.
- The existing 30-day retention rules, cloud-free operation, model selection,
  file format, clipboard fallback and accessibility insertion remain unchanged.

## Regression coverage

`AtomicRecordingMetadataTest` includes first creation, replacement of
existing metadata, simulated disk-full/partial write, failed final rename,
abandoned `.tmp` files and a non-directory parent.

## Remaining validation

- [ ] Android CI `assembleDebug`, `assembleDebugAndroidTest`, and
      `testDebugUnitTest` pass.
- [ ] Force a failed first sidecar write and confirm no incomplete recording
      appears in Home's retry list.
- [ ] Force a failed settlement write and confirm its WAV and prior JSON
      survive without unexpected pruning.
- [ ] Verify Home can still retrieve and retry unsettled recordings after a
      clean app restart.
- [ ] Test cancellation, successful clipboard fallback and service process
      death after model provisioning with networking disabled.
- [ ] Verify temporary sidecars are never listed as committed recordings.

**Scope:** This protects against interrupted metadata file replacement,
not every possible loss event. A power failure before directory metadata is
durably committed, physical storage failure, and unrelated corruption of an
already committed JSON file still require separate resilience measures.
Nor do JVM tests substitute for Android-device lifecycle validation.

If a terminal metadata write fails after text was already delivered, the
recording remains retryable; this is conservative against data loss but may
produce a duplicate retry entry. Distinguishing delivered-but-uncommitted
records requires a separate durable acknowledgement protocol.
