#!/usr/bin/env bash
DIR=$(realpath "$(cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )")
cd "$DIR/publish" || exit 1
npm i
"$DIR/publish/sh/publish.sh" "$(realpath $DIR/..)"