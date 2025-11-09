#!/usr/bin/env bash
set -euo pipefail

# Verify a XAdES-signed XML file end-to-end.
# Usage: ./verify.sh <signed_file.xml>
#
# Execution order and purpose:
#   1) Recompute document digest (URI="") and compare            (hash_document.sh)
#   2) Recompute SignedProperties digest and compare             (hash_properties.sh)
#   3) Extract embedded certificate & public key                 (signature_1.sh)
#   4) Canonicalize SignedInfo (exclusive C14N)                  (signature_2.sh)
#   5) Verify SignatureValue over SignedInfo                     (signature_3.sh)
#   6) Download issuer & root and verify certificate path        (certificate_1.sh)
#   7) Download CRL and check revocation                         (certificate_2.sh)
#   8) Check SigningTime within certificate validity window      (certificate_3.sh)

if [ $# -ne 1 ]; then
  echo "Usage: $0 <signed_file.xml>"
  exit 1
fi

FILE="$1"

# Clean temp/ at the beginning
rm -rf temp
mkdir -p temp

echo "==[1] Document digest (URI='') =="
bash ./hash_document.sh "$FILE"

echo "==[2] SignedProperties digest =="
bash ./hash_properties.sh "$FILE"

echo "==[3] Extract certificate & public key =="
bash ./signature_1.sh "$FILE"

echo "==[4] SignedInfo canonicalization =="
bash ./signature_2.sh "$FILE"

echo "==[5] SignatureValue verification =="
bash ./signature_3.sh "$FILE"

echo "==[6] Chain: issuer & root; path validation =="
bash ./certificate_1.sh "$FILE"

echo "==[7] CRL check =="
bash ./certificate_2.sh "$FILE"

echo "==[8] SigningTime within certificate validity =="
bash ./certificate_3.sh "$FILE"

echo "✅ Done."
