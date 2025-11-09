#!/usr/bin/env bash
set -euo pipefail

# STEP — Canonicalize SignedProperties node-set (exclusive C14N) and compute SHA-256
# Usage: ./hash_properties.sh <signed_file.xml>

if [ $# -ne 1 ]; then
  echo "Usage: $0 <signed_file.xml>"
  exit 1
fi

FILE="$1"
TEMPDIR="temp"
# 2) Extract SignedProperties Id
SP_ID="$(xmllint --xpath 'string(//*[local-name()="SignedProperties"]/@Id)' "$FILE")"
printf 'SP_ID=%s\n' "$SP_ID"

# 3) Build XPath selecting the correct node-set (nodes + attributes + namespaces)
cat > "$TEMPDIR/sp.xpath.xml" <<EOF
<?xml version="1.0"?>
<XPath>(//. | //@* | //namespace::*)[ancestor-or-self::*[@Id='$SP_ID']]</XPath>
EOF

# 4) Exclusive C14N (without comments) -> canonical bytes
xmlstarlet c14n --exc-without-comments "$FILE" "$TEMPDIR/sp.xpath.xml" > "$TEMPDIR/ref-signedprops.bin"

# 5) Compute SHA-256 and compare with value in the document
printf 'ACTUAL   SignedProperties: '
openssl dgst -sha256 -binary "$TEMPDIR/ref-signedprops.bin" | openssl base64 -A
printf '\n'

printf 'EXPECTED SignedProperties: '
xmllint --xpath "string(//*[local-name()='Reference'][@URI='#$SP_ID']/*[local-name()='DigestValue'])" "$FILE"
printf '\n'
