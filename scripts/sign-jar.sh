#!/usr/bin/env bash

. ./scripts/secrets.sh
cd build/libs

mkdir -p signed

signJar() {
  local file="$1"

  if [[ ! -f "$file" ]]; then
    echo "File '$file' not found"
    return 1
  fi

  echo "Signing $file"
  jarsigner -keystore "$JAVA_SIGNING_STORE" -signedjar "signed/$file" -storepass "$JAVA_SIGNING_PASS" "$file" "$JAVA_SIGNING_ALIAS" > /dev/null 2>&1
}

if [[ -n "$1" ]]; then
  if [ -f "signed/$1" ]; then
    echo "Removing signed/$1"
    rm "signed/$1"
  fi
  signJar "$1"
else
  rm -f signed/*.jar
  for file in *.jar; do
    if [ $file == "*.jar" ]; then
      echo "no file"
      exit 1
    fi

    signJar "$file"
  done
fi
