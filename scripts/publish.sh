#!/usr/bin/env bash
# usage: publish.sh [options], see src/publish/README.md or publish.sh --help
set -e
DIR=$(realpath "$(cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )")
if [[ -f "$DIR/secrets.sh" ]]; then
  set -a
  . "$DIR/secrets.sh"
  set +a
fi
cd "$DIR/../src/publish"
bun install --frozen-lockfile > /dev/null
exec bun src/index.ts "$@"
