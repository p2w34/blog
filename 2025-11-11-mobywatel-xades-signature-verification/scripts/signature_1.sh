#!/usr/bin/env bash
set -euo pipefail

# STEP 1 — Extract embedded certificate and public key from the signed XML
# Usage: ./signature_1.sh <signed_file.xml>

if [ $# -ne 1 ]; then
  echo "Usage: $0 <signed_file.xml>"
  exit 1
fi

FILE="$1"
TEMPDIR="temp"
echo "=== Tool versions (for reproducibility) ==="
echo "xmllint: $(command -v xmllint)"; xmllint --version | head -1 || true
echo "xmlstarlet: $(command -v xmlstarlet)"; xmlstarlet --version || true
echo "OpenSSL: $(command -v openssl)"; openssl version -a | head -1

# 1c) Extract embedded X.509 certificate from <ds:X509Certificate>
xmllint --xpath 'string(//*[local-name()="X509Certificate"][1])' "$FILE" > "$TEMPDIR/cert.b64"
openssl base64 -d -A -in "$TEMPDIR/cert.b64" -out "$TEMPDIR/cert.der"
openssl x509 -inform DER -in "$TEMPDIR/cert.der" -out "$TEMPDIR/cert.pem"

# 1d) Extract public key from certificate (used by OpenSSL verification)
openssl x509 -in "$TEMPDIR/cert.pem" -pubkey -noout > "$TEMPDIR/pubkey.pem"

# 1e) Short certificate summary (subject/issuer/dates/serial/fingerprint)
echo "=== Certificate summary ==="
openssl x509 -in "$TEMPDIR/cert.pem" -noout -subject -issuer -dates -serial -fingerprint -sha256
