#!/usr/bin/env bash
set -euo pipefail

# STEP — Download issuer and root certificates; verify chain for cert.pem
# Usage: ./certificate_1.sh <signed_file.xml>
# Note: Expects temp/cert.pem produced by signature_1.sh

if [ $# -ne 1 ]; then
  echo "Usage: $0 <signed_file.xml>"
  exit 1
fi

FILE="$1"
TEMPDIR="temp"
CERT="$TEMPDIR/cert.pem"

# 1) Issuer (EuroCert QCA03 – official link)
curl -sSL "https://eurocert.pl/pub/Prawo/QCA03_Eurocert_2017.der" -o "$TEMPDIR/issuer.der"
openssl x509 -inform DER -in "$TEMPDIR/issuer.der" -out "$TEMPDIR/issuer.pem"

# 2) Root NCCert (new, 2016–2039; link also shown on EuroCert website)
curl -sSL "https://www.nccert.pl/files/nccert2016.crt" -o "$TEMPDIR/nccert2016.crt"
# If file is already PEM – OK; if DER, convert:
openssl x509 -in "$TEMPDIR/nccert2016.crt" -out "$TEMPDIR/root.pem" 2>/dev/null || \
openssl x509 -inform DER -in "$TEMPDIR/nccert2016.crt" -out "$TEMPDIR/root.pem"

# 3) Path validation: end-entity -> QCA03 -> NCCert root
openssl verify -untrusted "$TEMPDIR/issuer.pem" -CAfile "$TEMPDIR/root.pem" "$CERT"
