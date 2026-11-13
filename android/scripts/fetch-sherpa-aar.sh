#!/usr/bin/env bash
# Reproducibly fetch the sherpa-onnx Android AAR used by both CI and release.
# Digest is the SHA-256 published with the upstream GitHub release asset v1.13.3.
set -euo pipefail

readonly NAME="sherpa-onnx-static-link-onnxruntime-1.13.3.aar"
readonly SHA256="9f065fe6f2cab09fd48eaa580097293e077637ad53a5e89c5c58a36509386ac7"
readonly URL="https://github.com/k2-fsa/sherpa-onnx/releases/download/v1.13.3/${NAME}"
readonly OUT="${1:-app/libs/${NAME}}"

mkdir -p "$(dirname "$OUT")"
if [[ -f "$OUT" ]]; then
    if printf '%s  %s\n' "$SHA256" "$OUT" | sha256sum --check --status; then
        echo "Verified cached ${NAME} (SHA-256 pinned to upstream release)"
        exit 0
    fi
    echo "Cached ${NAME} checksum mismatch; downloading a verified replacement" >&2
fi

TMP="$(mktemp "${OUT}.tmp.XXXXXXXX")"
trap 'rm -f "$TMP"' EXIT
curl --fail --location --show-error --silent --retry 3 --retry-delay 2 \
    --output "$TMP" "$URL"
printf '%s  %s\n' "$SHA256" "$TMP" | sha256sum --check
mv -f "$TMP" "$OUT"
echo "Fetched and SHA-256 verified ${NAME}"
