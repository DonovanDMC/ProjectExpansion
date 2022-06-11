#!/usr/bin/env bash
DIR=$(realpath "$(cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )")
cd "$DIR/../src/main/generation" || exit 1
npm i
node --no-warnings --no-deprecation --experimental-specifier-resolution=node --loader ts-node/esm run
