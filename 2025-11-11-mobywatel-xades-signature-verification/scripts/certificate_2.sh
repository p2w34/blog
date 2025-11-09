#!/usr/bin/env bash
set -euo pipefail

# STEP — Download CRL and check revocation for cert.pem against QCA03 CRL
# Usage: ./certificate_2.sh <signed_file.xml>
# Note: Expects temp/cert.pem, temp/issuer.pem and temp/root.pem

if [ $# -ne 1 ]; then
  echo "Usage: $0 <signed_file.xml>"
  exit 1
fi

FILE="$1"
TEMPDIR="temp"
# 1) Fetch CRL for QCA03
curl -sSL "http://crl.eurocert.pl/qca03.crl" -o "$TEMPDIR/qca03.crl"

# 2) Convert CRL to PEM (most CRLs are DER)
openssl crl -inform DER -in "$TEMPDIR/qca03.crl" -out "$TEMPDIR/qca03.pem"

# 3) Verify with CRL (checks whether temp/cert.pem appears on QCA03 list)
openssl verify -crl_check -CRLfile "$TEMPDIR/qca03.pem" \
  -untrusted "$TEMPDIR/issuer.pem" -CAfile "$TEMPDIR/root.pem" "$TEMPDIR/cert.pem"
