#!/usr/bin/env bash
set -euo pipefail

# STEP — Canonicalize the document excluding <ds:Signature> and compute SHA-256
# Usage: ./hash_document.sh <signed_file.xml>
# Produces ACTUAL digest (recomputed) and shows EXPECTED from the XML (URI="")

if [ $# -ne 1 ]; then
  echo "Usage: $0 <signed_file.xml>"
  exit 1
fi

FILE="$1"
TEMPDIR="temp"
# 2) Build XPath: everything except <ds:Signature> (nodes + attributes + namespaces)
cat > "$TEMPDIR/nosig.xpath.xml" <<'XML'
<?xml version="1.0"?>
<XPath>(//. | //@* | //namespace::*)[
  not(ancestor-or-self::*[
    local-name()='Signature' and namespace-uri()="http://www.w3.org/2000/09/xmldsig#"
  ])
]</XPath>
XML

# 3) Inclusive C14N (without comments) -> canonical bytes
xmlstarlet c14n --without-comments "$FILE" "$TEMPDIR/nosig.xpath.xml" > "$TEMPDIR/ref-doc-nosig.bin"

# 4) Compute SHA-256 (binary) -> Base64 (ACTUAL)
printf 'ACTUAL   URI='''':           '
openssl dgst -sha256 -binary "$TEMPDIR/ref-doc-nosig.bin" | openssl base64 -A
printf '\n'

# 5) Extract expected value from document (EXPECTED)
printf 'EXPECTED URI='''':           '
xmllint --xpath "string(//*[local-name()='Reference'][@URI='']/*[local-name()='DigestValue'])" "$FILE"
printf '\n'
