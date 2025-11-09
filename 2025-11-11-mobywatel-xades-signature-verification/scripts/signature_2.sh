#!/usr/bin/env bash
set -euo pipefail

# STEP 2 — Build canonical SignedInfo (si.bin) using exclusive C14N
# Usage: ./signature_2.sh <signed_file.xml>

if [ $# -ne 1 ]; then
  echo "Usage: $0 <signed_file.xml>"
  exit 1
fi

FILE="$1"
TEMPDIR="temp"
# XPath selects the full node-set (nodes, attributes, namespaces) under <ds:SignedInfo>
cat > "$TEMPDIR/si.xpath.xml" <<'XML'
<?xml version="1.0"?>
<XPath xmlns:ds="http://www.w3.org/2000/09/xmldsig#">
(//. | //@* | //namespace::*)[ancestor-or-self::ds:SignedInfo]
</XPath>
XML

# Exclusive C14N (without comments) -> si.bin
xmlstarlet c14n --exc-without-comments "$FILE" "$TEMPDIR/si.xpath.xml" > "$TEMPDIR/si.bin"

# Diagnostic: size
wc -c "$TEMPDIR/si.bin"
