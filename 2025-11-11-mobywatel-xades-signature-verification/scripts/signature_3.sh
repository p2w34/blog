#!/usr/bin/env bash
set -euo pipefail

# STEP 3 — Verify SignatureValue over SignedInfo (exclusive C14N)
# Usage: ./signature_3.sh <signed_file.xml>
# Notes:
#   - Expects temp/si.bin from signature_2.sh
#   - Uses temp/pubkey.pem extracted by signature_1.sh (RSA public key)

if [ $# -ne 1 ]; then
  echo "Usage: $0 <signed_file.xml>"
  exit 1
fi

FILE="$1"
TEMPDIR="temp"
PUBKEY="$TEMPDIR/pubkey.pem"

# Quick tool/version peek
xmlstarlet --version || true
openssl version -a | head -1

# Ensure we have canonical SignedInfo from signature_2.sh
[ -s "$TEMPDIR/si.bin" ] || { echo "Missing $TEMPDIR/si.bin — run signature_2.sh first."; exit 2; }

# Extract SignatureValue from XML (set ds namespace explicitly)
xmlstarlet sel -N ds="http://www.w3.org/2000/09/xmldsig#" \
  -t -v 'normalize-space(//ds:SignatureValue[1])' \
  "$FILE" > "$TEMPDIR/sig.b64"

# Decode Base64 → sig.bin and make sure it's not empty (RSA-2048 ≈ 256 bytes)
openssl base64 -d -A -in "$TEMPDIR/sig.b64" -out "$TEMPDIR/sig.bin"
wc -c "$TEMPDIR/sig.bin"

# RSA PKCS#1 v1.5 recover → compare hashes (a strong correctness test)
openssl pkeyutl -verifyrecover \
  -pubin -inkey "$PUBKEY" \
  -pkeyopt rsa_padding_mode:pkcs1 \
  -in "$TEMPDIR/sig.bin" -out "$TEMPDIR/rec.bin"

REC=$(tail -c 32 "$TEMPDIR/rec.bin" | openssl base64 -A)
OUR=$(openssl dgst -sha256 -binary "$TEMPDIR/si.bin" | openssl base64 -A)

printf "RECOVERED=%s\nOUR      =%s\n" "$REC" "$OUR"
[ "$REC" = "$OUR" ] && echo "✅ SignatureValue OK (hash matches)" || echo "❌ SignatureValue mismatch"
