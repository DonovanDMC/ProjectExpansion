#!/usr/bin/env bash
DIR=$(realpath "$(cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )")
cd $DIR/publish
# ESM & I can't be bothered to transpile
node --experimental-specifier-resolution=node --loader ts-node/esm run.ts