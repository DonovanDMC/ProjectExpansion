#!/usr/bin/env bash
# usage: generate-assets.sh <minecraft version> <loader> <output dir>
set -e
DIR=$(realpath "$(cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )")
OUT=$(realpath -m "$3")
cd "$DIR/../src/generation"
bun install --frozen-lockfile
bun src/index.ts "$1" "$OUT" --loader "$2"
