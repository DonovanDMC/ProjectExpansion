#!/usr/bin/env bash
# ESM & I can't be bothered to transpile
node --experimental-specifier-resolution=node  --loader ts-node/esm js/run.ts