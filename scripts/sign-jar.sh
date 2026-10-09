#!/usr/bin/env bash
# usage: sign-jar.sh <secrets file> <input jar> <output jar>
set -e

. "$1"
input="$2"
output="$3"

if [[ ! -f "$input" ]]; then
  echo "File '$input' not found"
  exit 1
fi

mkdir -p "$(dirname "$output")"
rm -f "$output"

echo "Signing $(basename "$input")"
"${JAVA_HOME:+$JAVA_HOME/bin/}jarsigner" -keystore "$JAVA_SIGNING_STORE" -signedjar "$output" -storepass "$JAVA_SIGNING_PASS" "$input" "$JAVA_SIGNING_ALIAS" > /dev/null 2>&1
