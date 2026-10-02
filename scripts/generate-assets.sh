#!/usr/bin/env bash
DIR=$(realpath "$(cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )")
cd "$DIR/../src/main/generation" || exit 1
bun install
bun run.ts
