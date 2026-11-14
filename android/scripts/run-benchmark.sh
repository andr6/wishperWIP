#!/usr/bin/env bash
# Run private, on-device ASR evaluation without any cloud inference.
# Usage: scripts/run-benchmark.sh <fixtures-dir> <results-dir> [models] [repeats]
set -euo pipefail

if [[ $# -lt 2 || $# -gt 4 ]]; then
    echo "Usage: $0 <fixtures-dir> <results-dir> [models] [repeats]" >&2
    exit 2
fi
command -v adb >/dev/null || { echo "adb is required" >&2; exit 2; }
FIXTURES="$(cd "$1" && pwd)"
OUTPUT="$2"
MODELS="${3:-parakeet,parakeet-v3,base}"
REPEATS="${4:-2}"
APP="com.voicerewriter"
DEST="/sdcard/Android/data/${APP}/files/benchmark"

[[ -s "${FIXTURES}/fixtures.tsv" ]] || { echo "Missing fixtures.tsv in $FIXTURES" >&2; exit 2; }
[[ "$MODELS" =~ ^[A-Za-z0-9,_-]+$ ]] || { echo "Invalid model list" >&2; exit 2; }
[[ "$REPEATS" =~ ^[1-9]$|^10$ ]] || { echo "repeats must be 1..10" >&2; exit 2; }
[[ "$(adb devices | awk '$2=="device"{count++} END{print count+0}')" == "1" ]] || {
    echo "Connect exactly one authorized Android device via ADB" >&2
    exit 2
}

mkdir -p "$OUTPUT"
echo "Uploading private fixtures to the app-specific external directory..."
adb shell mkdir -p "$DEST"
adb push "${FIXTURES}/." "$DEST/"
# Old JSON results cannot be confused with fresh results from a failed test.
adb shell rm -f "$DEST/results.jsonl" "$DEST/summary.json"

echo "Running offline ASR benchmark. Disable mobile data and Wi-Fi first."
set +e
adb shell am instrument -w \
    -e class com.voicerewriter.OnDeviceBenchmarkTest \
    -e models "$MODELS" \
    -e repeats "$REPEATS" \
    "${APP}.test/androidx.test.runner.AndroidJUnitRunner" | tee "$OUTPUT/instrumentation.txt"
status=${PIPESTATUS[0]}
set -e
if [[ "$status" -ne 0 ]] || ! grep -q "OK (1 test)" "$OUTPUT/instrumentation.txt"; then
    echo "Benchmark did not complete successfully; check instrumentation.txt" >&2
    exit 1
fi
adb pull "$DEST/results.jsonl" "$OUTPUT/results.jsonl"
adb pull "$DEST/summary.json" "$OUTPUT/summary.json"
echo "Benchmark results saved to: $OUTPUT"
echo "Do not commit private recordings or results to Git."
