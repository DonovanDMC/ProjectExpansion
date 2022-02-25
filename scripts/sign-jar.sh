#!/usr/bin/env bash

. ./scripts/secrets.sh
cd build/libs
mkdir -p signed
for file in *.jar; do
  if [ $file == "*.jar" ]; then
    echo "no file"
    exit 1
  fi

  jarsigner -keystore "$JAVA_SIGNING_STORE" -signedjar "signed/$file" -storepass "$JAVA_SIGNING_PASS" "$file" "$JAVA_SIGNING_ALIAS"
done