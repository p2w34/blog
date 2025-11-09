#!/usr/bin/env bash
set -euo pipefail

# STEP — Compare XAdES <SigningTime> against certificate validity period
# Usage: ./certificate_3.sh <signed_file.xml>
# Notes:
#  - Works on macOS 'date' (BSD). For GNU date, formats differ.
#  - Requires temp/cert.pem from signature_1.sh

if [ $# -ne 1 ]; then
  echo "Usage: $0 <signed_file.xml>"
  exit 1
fi

FILE="$1"
TEMPDIR="temp"
echo "FILE=$FILE"

# 1) Extract SigningTime from XAdES
SigningTime="$(xmlstarlet sel -t -v 'normalize-space((//*[local-name()="SigningTime"])[1])' "$FILE")"
echo "SigningTime: $SigningTime"

# Normalize ISO-8601: drop milliseconds and colon in timezone (BSD date dislikes it)
ST_NOMS="$(printf '%s' "$SigningTime" | sed -E 's/\.[0-9]+//; s/([+-][0-9]{2}):([0-9]{2})/\1\2/')"
ST_EPOCH="$(LC_ALL=C date -j -f '%Y-%m-%dT%H:%M:%S%z' "$ST_NOMS" +%s)"

# 2) Certificate validity dates (we already have temp/cert.pem)
NB="$(openssl x509 -in "$TEMPDIR/cert.pem" -noout -startdate | sed 's/^notBefore=//')"
NA="$(openssl x509 -in "$TEMPDIR/cert.pem" -noout -enddate   | sed 's/^notAfter=//')"

# Force C-locale when parsing "Feb ..." from OpenSSL
NB_EPOCH="$(LC_ALL=C date -j -f '%b %e %H:%M:%S %Y %Z' "$NB" +%s)"
NA_EPOCH="$(LC_ALL=C date -j -f '%b %e %H:%M:%S %Y %Z' "$NA" +%s)"

printf 'NB=%s (%s)\nNA=%s (%s)\nST=%s (%s)\n' "$NB" "$NB_EPOCH" "$NA" "$NA_EPOCH" "$SigningTime" "$ST_EPOCH"

if [ "$ST_EPOCH" -ge "$NB_EPOCH" ] && [ "$ST_EPOCH" -le "$NA_EPOCH" ]; then
  echo "✅ SigningTime is within certificate validity"
else
  echo "❌ SigningTime is outside certificate validity"
fi
