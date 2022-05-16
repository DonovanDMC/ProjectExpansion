#!/usr/bin/env bash
DIR=$(realpath "$(cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )")
"$DIR/publish/sh/publish.sh"